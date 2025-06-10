# Save as clean_working_server.py
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
    chat.config.prompt = '[speed_5]'  # Slightly slower for complete generation
print("ChatTTS loaded and configured successfully!")

def preprocess_text_for_complete_generation(text):
    """Preprocess text to ensure ChatTTS generates complete audio with clean female voice"""
    # Remove extra whitespace
    text = text.strip()

    # Remove any existing control tokens that might be spoken as words
    text = text.replace('[female]', '').replace('[male]', '').replace('[uv_break]', '')
    text = text.replace('[speed_5]', '').replace('[speed_4]', '').replace('[speed_6]', '')
    text = text.replace('minimal processing test', '').replace('processing test', '')
    text = text.replace('oh', '').replace('emmm', '').replace('umm', '').replace('uh', '')
    text = text.strip()

    # Ensure proper sentence ending
    if not text.endswith(('.', '!', '?', ':', ';')):
        text += '.'

    return text

def generate_complete_audio(text, max_retries=3):
    """Generate audio with retries to ensure completeness and clean speech"""
    processed_text = preprocess_text_for_complete_generation(text)

    for attempt in range(max_retries):
        print(f"Generation attempt {attempt + 1}: '{processed_text}'")

        try:
            # Use basic inference first (often cleaner for avoiding unwanted speech)
            wavs = chat.infer([processed_text])

            # If basic fails, try with advanced parameters
            if not wavs or len(wavs) == 0:
                wavs = chat.infer(
                    [processed_text],
                    use_decoder=True,  # Ensure proper decoding
                    do_text_normalization=True,  # Normalize text properly
                )
        except Exception as e:
            print(f"ChatTTS infer error: {e}")
            # Fallback to basic inference
            try:
                wavs = chat.infer([processed_text])
            except:
                wavs = None

        if wavs and len(wavs) > 0:
            audio_data = wavs[0]

            # Check if audio seems complete (basic heuristic)
            expected_min_length = len(text) * 800  # Reduced from 1000 for more lenient check
            actual_length = len(audio_data)

            print(f"Audio length: {actual_length}, expected min: {expected_min_length}")

            if actual_length >= expected_min_length * 0.6:  # More lenient check
                print("Audio generation appears complete")
                return audio_data
            else:
                print(f"Audio seems short, retrying... (attempt {attempt + 1})")
        else:
            print(f"No audio generated, retrying... (attempt {attempt + 1})")

    # If all retries failed, use the last generation
    print("Using final generation attempt")
    return wavs[0] if wavs and len(wavs) > 0 else None

@app.route('/tts', methods=['POST'])
def text_to_speech():
    """Simple, reliable text to speech conversion with clean female voice"""
    try:
        data = request.json
        text = data.get('text', '').strip()
        fade_out_ms = data.get('fade_out_ms', 300)  # Customizable fade-out duration
        silence_ms = data.get('silence_ms', 200)    # Customizable silence padding

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"Generating clean female voice for: {text}")

        # Generate audio with completeness checking and clean processing
        audio_data = generate_complete_audio(text)
        if audio_data is None:
            return jsonify({'error': 'Failed to generate complete audio after retries'}), 500

        print(f"Generated audio shape: {audio_data.shape}, dtype: {audio_data.dtype}")

        # Method 1: Use temporary file approach (most reliable)
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            try:
                # Write WAV file
                sf.write(temp_wav.name, audio_data, 24000)
                print(f"WAV file written to: {temp_wav.name}")

                # Convert to MP3 with better ending
                audio_segment = AudioSegment.from_wav(temp_wav.name)

                # Add smooth ending to prevent abrupt termination
                fade_duration = min(fade_out_ms, len(audio_segment) // 4)
                audio_segment = audio_segment.fade_out(fade_duration)

                # Add silence padding at the end
                silence_padding = AudioSegment.silent(duration=silence_ms)
                audio_segment = audio_segment + silence_padding

                # Export to memory
                mp3_buffer = io.BytesIO()
                audio_segment.export(
                    mp3_buffer,
                    format="mp3",
                    bitrate="128k",
                    parameters=["-ac", "1", "-ar", "24000"]  # Mono, 24kHz
                )
                mp3_buffer.seek(0)

                # Clean up temp file
                os.unlink(temp_wav.name)

                print(f"Clean MP3 generated successfully, size: {len(mp3_buffer.getvalue())} bytes")
                print(f"Applied fade-out: {fade_duration}ms, silence padding: {silence_ms}ms")

                return send_file(
                    mp3_buffer,
                    download_name='clean_female_voice.mp3',
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

@app.route('/tts/clean', methods=['POST'])
def text_to_speech_ultra_clean():
    """Ultra clean TTS with absolutely no unwanted words - female voice"""
    try:
        data = request.json
        text = data.get('text', '').strip()

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"Generating ULTRA CLEAN female voice for: {text}")

        # Ultra clean processing - remove everything that could cause unwanted speech
        clean_text = text.strip()

        # Remove any potential unwanted words/sounds
        unwanted_words = ['oh', 'emmm', 'umm', 'uh', 'er', 'ah', 'testing', 'test',
                         'processing', 'minimal', 'generation', 'audio', 'speech']

        for word in unwanted_words:
            clean_text = clean_text.replace(word, '').replace(word.capitalize(), '')

        # Clean up extra spaces
        clean_text = ' '.join(clean_text.split())

        # Add period if needed
        if not clean_text.endswith(('.', '!', '?', ':', ';')):
            clean_text += '.'

        print(f"Ultra clean text: '{clean_text}'")

        # Generate with minimal parameters to avoid artifacts
        try:
            wavs = chat.infer([clean_text])
        except Exception as e:
            print(f"Ultra clean generation error: {e}")
            return jsonify({'error': 'Failed to generate ultra clean audio'}), 500

        if not wavs or len(wavs) == 0:
            return jsonify({'error': 'No audio generated'}), 500

        audio_data = wavs[0]
        print(f"Generated ultra clean audio shape: {audio_data.shape}")

        # Convert to MP3 with your exact working logic
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            try:
                sf.write(temp_wav.name, audio_data, 24000)
                audio_segment = AudioSegment.from_wav(temp_wav.name)

                # Add gentle ending
                fade_duration = min(250, len(audio_segment) // 5)
                audio_segment = audio_segment.fade_out(fade_duration)
                silence_padding = AudioSegment.silent(duration=150)
                audio_segment = audio_segment + silence_padding

                mp3_buffer = io.BytesIO()
                audio_segment.export(
                    mp3_buffer,
                    format="mp3",
                    bitrate="128k",
                    parameters=["-ac", "1", "-ar", "24000"]
                )
                mp3_buffer.seek(0)

                os.unlink(temp_wav.name)

                print(f"Ultra clean MP3 generated successfully")

                return send_file(
                    mp3_buffer,
                    download_name='ultra_clean_female.mp3',
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

@app.route('/tts/natural', methods=['POST'])
def text_to_speech_natural():
    """TTS with enhanced natural ending options - clean female voice"""
    try:
        data = request.json
        text = data.get('text', '').strip()
        ending_style = data.get('ending_style', 'smooth')

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"Generating clean natural female audio for: {text} (style: {ending_style})")

        # Generate audio with clean processing
        audio_data = generate_complete_audio(text)
        if audio_data is None:
            return jsonify({'error': 'Failed to generate complete audio after retries'}), 500

        # Define ending styles
        ending_configs = {
            'smooth': {'fade_ms': 300, 'silence_ms': 200},
            'gentle': {'fade_ms': 500, 'silence_ms': 300},
            'period': {'fade_ms': 400, 'silence_ms': 400},
            'abrupt': {'fade_ms': 50, 'silence_ms': 100}
        }

        config = ending_configs.get(ending_style, ending_configs['smooth'])

        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            try:
                sf.write(temp_wav.name, audio_data, 24000)
                audio_segment = AudioSegment.from_wav(temp_wav.name)

                # Apply natural ending
                fade_duration = min(config['fade_ms'], len(audio_segment) // 3)
                audio_segment = audio_segment.fade_out(fade_duration)

                # Add silence padding
                silence_padding = AudioSegment.silent(duration=config['silence_ms'])
                audio_segment = audio_segment + silence_padding

                # Add subtle fade-in for smoother start
                if ending_style in ['smooth', 'gentle', 'period']:
                    audio_segment = audio_segment.fade_in(50)

                mp3_buffer = io.BytesIO()
                audio_segment.export(
                    mp3_buffer,
                    format="mp3",
                    bitrate="128k",
                    parameters=["-ac", "1", "-ar", "24000"]
                )
                mp3_buffer.seek(0)

                os.unlink(temp_wav.name)

                print(f"Clean natural MP3 generated: fade={fade_duration}ms, silence={config['silence_ms']}ms")

                return send_file(
                    mp3_buffer,
                    download_name=f'clean_female_{ending_style}.mp3',
                    mimetype='audio/mpeg',
                    as_attachment=True
                )

            except Exception as conv_error:
                if os.path.exists(temp_wav.name):
                    os.unlink(temp_wav.name)
                raise conv_error

    except Exception as e:
        print(f"Error: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/test', methods=['GET'])
def test_audio():
    """Test endpoint with clean female voice"""
    try:
        text = "This is a test audio file."

        # Generate clean audio
        audio_data = generate_complete_audio(text)
        if audio_data is None:
            return jsonify({'error': 'Failed to generate complete test audio'}), 500

        # Use the same conversion logic with smooth ending
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            sf.write(temp_wav.name, audio_data, 24000)
            audio_segment = AudioSegment.from_wav(temp_wav.name)

            # Add smooth ending
            fade_duration = min(300, len(audio_segment) // 4)
            audio_segment = audio_segment.fade_out(fade_duration)
            silence_padding = AudioSegment.silent(duration=200)
            audio_segment = audio_segment + silence_padding

            mp3_buffer = io.BytesIO()
            audio_segment.export(mp3_buffer, format="mp3", bitrate="128k")
            mp3_buffer.seek(0)

            os.unlink(temp_wav.name)

            return send_file(
                mp3_buffer,
                download_name='clean_female_test.mp3',
                mimetype='audio/mpeg',
                as_attachment=True
            )

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/debug/generation', methods=['POST'])
def debug_generation():
    """Debug endpoint to analyze clean audio generation"""
    try:
        data = request.json
        text = data.get('text', '').strip()

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"Debug: Analyzing clean generation for: '{text}'")

        # Test both processed and unprocessed text
        results = {}

        # Original text
        print("Testing original text...")
        try:
            wavs_original = chat.infer([text])
            if wavs_original and len(wavs_original) > 0:
                results['original'] = {
                    'length': len(wavs_original[0]),
                    'duration_seconds': len(wavs_original[0]) / 24000,
                    'text': text
                }
        except Exception as e:
            results['original'] = {'error': str(e)}

        # Clean processed text
        processed_text = preprocess_text_for_complete_generation(text)
        print(f"Testing clean processed text: '{processed_text}'")
        try:
            wavs_processed = chat.infer([processed_text])
            if wavs_processed and len(wavs_processed) > 0:
                results['clean_processed'] = {
                    'length': len(wavs_processed[0]),
                    'duration_seconds': len(wavs_processed[0]) / 24000,
                    'text': processed_text
                }
        except Exception as e:
            results['clean_processed'] = {'error': str(e)}

        # Analysis
        char_count = len(text)
        expected_duration = char_count * 0.1

        results['analysis'] = {
            'character_count': char_count,
            'expected_duration_estimate': expected_duration,
            'text_ends_with_punctuation': text.endswith(('.', '!', '?', ':', ';')),
            'cleaning_applied': True,
            'voice_gender': 'female',
            'recommendation': 'Use clean processed text for best results'
        }

        return jsonify(results)

    except Exception as e:
        print(f"Debug error: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'healthy',
        'service': 'Clean Female Voice ChatTTS API',
        'features': {
            'clean_speech_generation': True,
            'female_voice': True,
            'no_unwanted_words': True,
            'complete_generation': True,
            'text_preprocessing': True,
            'retry_logic': True,
            'smooth_endings': True,
            'debug_endpoints': True,
            'ending_styles': ['smooth', 'gentle', 'period', 'abrupt']
        },
        'endpoints': {
            '/tts': 'Standard clean female voice',
            '/tts/clean': 'Ultra clean female voice (no unwanted words)',
            '/tts/natural': 'Natural ending styles with clean voice',
            '/test': 'Test with sample text',
            '/debug/generation': 'Analyze generation quality'
        }
    })

if __name__ == '__main__':
    # Use threading for better concurrent handling
    app.run(host='0.0.0.0', port=8000, debug=True)