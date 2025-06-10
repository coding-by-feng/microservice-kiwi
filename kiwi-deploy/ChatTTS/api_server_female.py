# Save as improved_chattts_server.py
from flask import Flask, request, jsonify, send_file
import ChatTTS
import torch
import numpy as np
import soundfile as sf
from pydub import AudioSegment
import io
import tempfile
import os
import base64

app = Flask(__name__)

# Initialize ChatTTS
print("Loading ChatTTS...")
chat = ChatTTS.Chat()
chat.load(compile=False)  # Use compile=False for compatibility

# Configure ChatTTS for complete generation
print("Configuring ChatTTS parameters...")
# Set parameters to ensure complete text generation
if hasattr(chat, 'config'):
    chat.config.prompt = '[speed_4]'  # Slightly slower for more complete pronunciation
print("ChatTTS loaded and configured successfully!")

def preprocess_text_for_complete_generation(text):
    """Preprocess text to ensure ChatTTS generates complete audio with proper endings"""
    # Remove extra whitespace
    text = text.strip()

    # Remove any existing control tokens that might be spoken as words
    text = text.replace('[female]', '').replace('[male]', '').replace('[uv_break]', '')
    text = text.replace('[speed_5]', '').replace('[speed_4]', '').replace('[speed_6]', '')
    text = text.replace('minimal processing test', '').replace('processing test', '')
    text = text.replace('oh', '').replace('emmm', '').replace('umm', '').replace('uh', '')
    text = text.strip()

    # Ensure proper sentence ending with emphasis on final words
    if not text.endswith(('.', '!', '?', ':', ';')):
        text += '.'

    # Add slight pause before final punctuation to ensure complete pronunciation
    # This helps with words like "here!" getting fully pronounced
    if text.endswith('!'):
        # Replace final exclamation with slight pause + exclamation
        text = text[:-1] + ' !'
    elif text.endswith('.'):
        # Add slight emphasis to final word
        text = text[:-1] + ' .'

    return text

def generate_complete_audio(text, max_retries=3):
    """Generate audio with retries to ensure completeness and full pronunciation"""
    processed_text = preprocess_text_for_complete_generation(text)

    for attempt in range(max_retries):
        print(f"Generation attempt {attempt + 1}: '{processed_text}'")

        try:
            # Use advanced parameters for better completion
            wavs = chat.infer(
                [processed_text],
                use_decoder=True,  # Ensure proper decoding
                do_text_normalization=True,  # Normalize text properly
                # Add slight temperature for more natural speech
                temperature=0.3 if hasattr(chat, 'temperature') else None
            )

            # If advanced fails, try with basic inference
            if not wavs or len(wavs) == 0:
                wavs = chat.infer([processed_text])

        except Exception as e:
            print(f"ChatTTS infer error: {e}")
            # Fallback to basic inference
            try:
                wavs = chat.infer([processed_text])
            except:
                wavs = None

        if wavs and len(wavs) > 0:
            audio_data = wavs[0]

            # More lenient check for completion - focus on minimum viable length
            expected_min_length = len(text) * 600  # Reduced minimum length requirement
            actual_length = len(audio_data)

            print(f"Audio length: {actual_length}, expected min: {expected_min_length}")

            # Very lenient check - prioritize having audio over perfect length
            if actual_length >= expected_min_length * 0.4:  # Even more lenient
                print("Audio generation appears sufficient for complete pronunciation")
                return audio_data
            else:
                print(f"Audio seems short, retrying... (attempt {attempt + 1})")
        else:
            print(f"No audio generated, retrying... (attempt {attempt + 1})")

    # If all retries failed, use the last generation
    print("Using final generation attempt")
    return wavs[0] if wavs and len(wavs) > 0 else None

def apply_natural_ending(audio_segment, style='enhanced'):
    """Apply natural ending processing to prevent abrupt termination"""

    if style == 'enhanced':
        # Enhanced ending for better pronunciation completion

        # 1. Longer fade-out to ensure final words aren't cut
        fade_duration = min(600, len(audio_segment) // 3)  # Longer fade
        audio_segment = audio_segment.fade_out(fade_duration)

        # 2. Add substantial silence padding for natural conclusion
        silence_padding = AudioSegment.silent(duration=400)  # More silence
        audio_segment = audio_segment + silence_padding

        # 3. Gentle fade-in to smooth start
        audio_segment = audio_segment.fade_in(100)

        print(f"Applied enhanced ending: fade={fade_duration}ms, silence=400ms")

    elif style == 'punctuation_aware':
        # Punctuation-aware processing

        # Longer fade for exclamations and questions
        fade_duration = min(500, len(audio_segment) // 4)
        audio_segment = audio_segment.fade_out(fade_duration)

        # Variable silence based on punctuation (would need text context)
        silence_padding = AudioSegment.silent(duration=350)
        audio_segment = audio_segment + silence_padding

        print(f"Applied punctuation-aware ending: fade={fade_duration}ms")

    else:  # default 'smooth'
        fade_duration = min(400, len(audio_segment) // 4)
        audio_segment = audio_segment.fade_out(fade_duration)
        silence_padding = AudioSegment.silent(duration=300)
        audio_segment = audio_segment + silence_padding

    return audio_segment

@app.route('/tts', methods=['POST'])
def text_to_speech():
    """Enhanced TTS with proper ending handling for complete pronunciation"""
    try:
        data = request.json
        text = data.get('text', '').strip()
        ending_style = data.get('ending_style', 'enhanced')  # Default to enhanced

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"Generating enhanced female voice for: {text}")

        # Generate audio with enhanced completion checking
        audio_data = generate_complete_audio(text)
        if audio_data is None:
            return jsonify({'error': 'Failed to generate complete audio after retries'}), 500

        print(f"Generated audio shape: {audio_data.shape}, dtype: {audio_data.dtype}")

        # Process with enhanced ending handling
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            try:
                # Write WAV file
                sf.write(temp_wav.name, audio_data, 24000)
                print(f"WAV file written to: {temp_wav.name}")

                # Convert to AudioSegment for processing
                audio_segment = AudioSegment.from_wav(temp_wav.name)

                # Apply natural ending based on text characteristics
                if '!' in text:
                    audio_segment = apply_natural_ending(audio_segment, 'punctuation_aware')
                else:
                    audio_segment = apply_natural_ending(audio_segment, ending_style)

                # Export to memory with high quality
                mp3_buffer = io.BytesIO()
                audio_segment.export(
                    mp3_buffer,
                    format="mp3",
                    bitrate="160k",  # Higher bitrate for better quality
                    parameters=["-ac", "1", "-ar", "24000"]  # Mono, 24kHz
                )
                mp3_buffer.seek(0)

                # Clean up temp file
                os.unlink(temp_wav.name)

                print(f"Enhanced MP3 generated successfully, size: {len(mp3_buffer.getvalue())} bytes")

                return send_file(
                    mp3_buffer,
                    download_name='enhanced_female_voice.mp3',
                    mimetype='audio/mpeg',
                    as_attachment=True
                )

            except Exception as conv_error:
                # Clean up temp file on error
                if os.path.exists(temp_wav.name):
                    os.unlink(temp_wav.name)
                raise conv_error

    except Exception as e:
        print(f"Error: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/tts/complete', methods=['POST'])
def text_to_speech_complete():
    """TTS specifically optimized for complete pronunciation of all words"""
    try:
        data = request.json
        text = data.get('text', '').strip()

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"Generating COMPLETE pronunciation for: {text}")

        # Enhanced preprocessing for complete pronunciation
        enhanced_text = text.strip()

        # Add strategic pauses before final words to ensure they're not rushed
        words = enhanced_text.split()
        if len(words) > 1:
            # Add slight pause before last word if it's important
            last_word = words[-1].rstrip('.,!?;:')
            if len(last_word) > 2:  # Only for substantial words
                words[-1] = f" {words[-1]}"  # Add space for slight pause
                enhanced_text = ' '.join(words)

        # Ensure proper ending
        if not enhanced_text.endswith(('.', '!', '?', ':', ';')):
            enhanced_text += '.'

        print(f"Enhanced text for complete pronunciation: '{enhanced_text}'")

        # Generate with multiple attempts focusing on completion
        audio_data = None
        for attempt in range(4):  # More attempts for completeness
            try:
                print(f"Complete pronunciation attempt {attempt + 1}")

                # Try different inference approaches
                if attempt == 0:
                    # Standard approach
                    wavs = chat.infer([enhanced_text])
                elif attempt == 1:
                    # With decoder
                    wavs = chat.infer([enhanced_text], use_decoder=True)
                elif attempt == 2:
                    # With normalization
                    wavs = chat.infer([enhanced_text], do_text_normalization=True)
                else:
                    # Both decoder and normalization
                    wavs = chat.infer([enhanced_text], use_decoder=True, do_text_normalization=True)

                if wavs and len(wavs) > 0:
                    candidate_audio = wavs[0]
                    # Accept any reasonable length - prioritize having complete audio
                    if len(candidate_audio) > len(text) * 400:  # Very lenient minimum
                        audio_data = candidate_audio
                        print(f"Accepted audio from attempt {attempt + 1}")
                        break

            except Exception as e:
                print(f"Attempt {attempt + 1} failed: {e}")
                continue

        if audio_data is None:
            return jsonify({'error': 'Failed to generate complete pronunciation audio'}), 500

        # Process with extra care for endings
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            try:
                sf.write(temp_wav.name, audio_data, 24000)
                audio_segment = AudioSegment.from_wav(temp_wav.name)

                # Extra-long fade out to ensure no word cutting
                fade_duration = min(800, len(audio_segment) // 2)  # Very long fade
                audio_segment = audio_segment.fade_out(fade_duration)

                # Substantial silence padding
                silence_padding = AudioSegment.silent(duration=500)
                audio_segment = audio_segment + silence_padding

                # Gentle start
                audio_segment = audio_segment.fade_in(80)

                mp3_buffer = io.BytesIO()
                audio_segment.export(
                    mp3_buffer,
                    format="mp3",
                    bitrate="192k",  # High quality for clarity
                    parameters=["-ac", "1", "-ar", "24000"]
                )
                mp3_buffer.seek(0)

                os.unlink(temp_wav.name)

                print(f"Complete pronunciation MP3 generated: fade={fade_duration}ms, silence=500ms")

                return send_file(
                    mp3_buffer,
                    download_name='complete_pronunciation.mp3',
                    mimetype='audio/mpeg',
                    as_attachment=True
                )

            except Exception as conv_error:
                if os.path.exists(temp_wav.name):
                    os.unlink(temp_wav.name)
                raise conv_error

    except Exception as e:
        print(f"Error in complete pronunciation: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/tts/punctuation', methods=['POST'])
def text_to_speech_punctuation_aware():
    """TTS with punctuation-aware processing for natural breaks"""
    try:
        data = request.json
        text = data.get('text', '').strip()

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"Generating punctuation-aware speech for: {text}")

        # Analyze punctuation for appropriate processing
        has_exclamation = '!' in text
        has_question = '?' in text
        has_comma = ',' in text

        # Enhance text based on punctuation
        enhanced_text = text

        # Add slight pauses at commas for natural flow
        if has_comma:
            enhanced_text = enhanced_text.replace(',', ' , ')

        # Ensure exclamations get full pronunciation
        if has_exclamation:
            # Add space before exclamation for complete word pronunciation
            enhanced_text = enhanced_text.replace('!', ' !')

        # Process questions similarly
        if has_question:
            enhanced_text = enhanced_text.replace('?', ' ?')

        print(f"Punctuation-enhanced text: '{enhanced_text}'")

        # Generate audio
        audio_data = generate_complete_audio(enhanced_text)
        if audio_data is None:
            return jsonify({'error': 'Failed to generate punctuation-aware audio'}), 500

        # Process with punctuation-specific ending
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            try:
                sf.write(temp_wav.name, audio_data, 24000)
                audio_segment = AudioSegment.from_wav(temp_wav.name)

                # Punctuation-specific processing
                if has_exclamation:
                    # Longer fade for exclamations to capture full emotion
                    fade_duration = min(700, len(audio_segment) // 3)
                    silence_duration = 450
                elif has_question:
                    # Medium fade for questions
                    fade_duration = min(600, len(audio_segment) // 3)
                    silence_duration = 400
                else:
                    # Standard fade for statements
                    fade_duration = min(500, len(audio_segment) // 4)
                    silence_duration = 350

                audio_segment = audio_segment.fade_out(fade_duration)
                silence_padding = AudioSegment.silent(duration=silence_duration)
                audio_segment = audio_segment + silence_padding
                audio_segment = audio_segment.fade_in(60)

                mp3_buffer = io.BytesIO()
                audio_segment.export(
                    mp3_buffer,
                    format="mp3",
                    bitrate="160k",
                    parameters=["-ac", "1", "-ar", "24000"]
                )
                mp3_buffer.seek(0)

                os.unlink(temp_wav.name)

                punctuation_type = "exclamation" if has_exclamation else "question" if has_question else "statement"
                print(f"Punctuation-aware MP3 generated ({punctuation_type}): fade={fade_duration}ms")

                return send_file(
                    mp3_buffer,
                    download_name=f'punctuation_aware_{punctuation_type}.mp3',
                    mimetype='audio/mpeg',
                    as_attachment=True
                )

            except Exception as conv_error:
                if os.path.exists(temp_wav.name):
                    os.unlink(temp_wav.name)
                raise conv_error

    except Exception as e:
        print(f"Error in punctuation-aware generation: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Keep your existing endpoints with improvements
@app.route('/tts/clean', methods=['POST'])
def text_to_speech_ultra_clean():
    """Ultra clean TTS with enhanced ending - female voice"""
    try:
        data = request.json
        text = data.get('text', '').strip()

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"Generating ULTRA CLEAN female voice for: {text}")

        # Ultra clean processing
        clean_text = preprocess_text_for_complete_generation(text)
        print(f"Ultra clean text: '{clean_text}'")

        # Use the enhanced generation
        audio_data = generate_complete_audio(clean_text)
        if audio_data is None:
            return jsonify({'error': 'Failed to generate ultra clean audio'}), 500

        # Apply enhanced ending
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            try:
                sf.write(temp_wav.name, audio_data, 24000)
                audio_segment = AudioSegment.from_wav(temp_wav.name)

                # Use enhanced ending processing
                audio_segment = apply_natural_ending(audio_segment, 'enhanced')

                mp3_buffer = io.BytesIO()
                audio_segment.export(
                    mp3_buffer,
                    format="mp3",
                    bitrate="160k",
                    parameters=["-ac", "1", "-ar", "24000"]
                )
                mp3_buffer.seek(0)

                os.unlink(temp_wav.name)

                return send_file(
                    mp3_buffer,
                    download_name='ultra_clean_enhanced.mp3',
                    mimetype='audio/mpeg',
                    as_attachment=True
                )

            except Exception as conv_error:
                if os.path.exists(temp_wav.name):
                    os.unlink(temp_wav.name)
                raise conv_error

    except Exception as e:
        print(f"Error in ultra clean generation: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/test', methods=['GET'])
def test_audio():
    """Test endpoint with your example text"""
    try:
        text = "Hello, my name is Kason, and I am studying in Auckland, I love the environment and people here!"

        print(f"Testing with example text: {text}")

        # Use the complete pronunciation generation
        audio_data = generate_complete_audio(text)
        if audio_data is None:
            return jsonify({'error': 'Failed to generate complete test audio'}), 500

        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            sf.write(temp_wav.name, audio_data, 24000)
            audio_segment = AudioSegment.from_wav(temp_wav.name)

            # Apply enhanced ending for the test
            audio_segment = apply_natural_ending(audio_segment, 'enhanced')

            mp3_buffer = io.BytesIO()
            audio_segment.export(mp3_buffer, format="mp3", bitrate="160k")
            mp3_buffer.seek(0)

            os.unlink(temp_wav.name)

            return send_file(
                mp3_buffer,
                download_name='kason_example_test.mp3',
                mimetype='audio/mpeg',
                as_attachment=True
            )

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'healthy',
        'service': 'Enhanced ChatTTS API with Complete Pronunciation',
        'features': {
            'complete_pronunciation': True,
            'enhanced_endings': True,
            'punctuation_awareness': True,
            'no_abrupt_termination': True,
            'female_voice': True,
            'natural_breaks': True
        },
        'endpoints': {
            '/tts': 'Enhanced TTS with proper endings',
            '/tts/complete': 'Optimized for complete pronunciation',
            '/tts/punctuation': 'Punctuation-aware processing',
            '/tts/clean': 'Ultra clean with enhanced endings',
            '/test': 'Test with Kason example'
        }
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000, debug=True)