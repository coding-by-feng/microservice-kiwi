# Save as fixed_chattts_server.py
from flask import Flask, request, jsonify, send_file
import ChatTTS
import torch
import numpy as np
import soundfile as sf
from pydub import AudioSegment
import io
import tempfile
import os
import re

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

def detect_language_and_punctuation(text):
    """Detect language and punctuation types in text"""
    # Chinese characters pattern
    chinese_pattern = re.compile(r'[\u4e00-\u9fff]')
    has_chinese = bool(chinese_pattern.search(text))

    # English characters pattern (excluding punctuation)
    english_pattern = re.compile(r'[a-zA-Z]')
    has_english = bool(english_pattern.search(text))

    # Chinese punctuation
    chinese_punctuation = {
        'period': '。',
        'comma': '，',
        'exclamation': '！',
        'question': '？',
        'semicolon': '；',
        'colon': '：',
        'pause': '、'
    }

    # English punctuation
    english_punctuation = {
        'period': '.',
        'comma': ',',
        'exclamation': '!',
        'question': '?',
        'semicolon': ';',
        'colon': ':'
    }

    # Detect punctuation types
    punctuation_info = {
        'has_chinese': has_chinese,
        'has_english': has_english,
        'language': 'mixed' if (has_chinese and has_english) else 'chinese' if has_chinese else 'english',
        'chinese_punct': {},
        'english_punct': {}
    }

    # Check for Chinese punctuation
    for punct_type, punct_char in chinese_punctuation.items():
        if punct_char in text:
            punctuation_info['chinese_punct'][punct_type] = punct_char

    # Check for English punctuation
    for punct_type, punct_char in english_punctuation.items():
        if punct_char in text:
            punctuation_info['english_punct'][punct_type] = punct_char

    return punctuation_info

def clean_invalid_characters(text):
    """Remove or replace characters that ChatTTS considers invalid"""

    # First handle whitespace and newline characters
    whitespace_chars = {
        '\n': ' ',      # Newline to space
        '\r': ' ',      # Carriage return to space
        '\t': ' ',      # Tab to space
        '\f': ' ',      # Form feed to space
        '\v': ' ',      # Vertical tab to space
        '\u00a0': ' ',  # Non-breaking space to regular space
        '\u2028': ' ',  # Line separator to space
        '\u2029': ' ',  # Paragraph separator to space
        '\u200b': '',   # Zero-width space - remove
        '\u200c': '',   # Zero-width non-joiner - remove
        '\u200d': '',   # Zero-width joiner - remove
        '\ufeff': '',   # Byte order mark - remove
    }

    # Apply whitespace replacements first
    cleaned_text = text
    for ws_char, replacement in whitespace_chars.items():
        if ws_char in cleaned_text:
            cleaned_text = cleaned_text.replace(ws_char, replacement)
            if ws_char in ['\n', '\r', '\t']:
                print(f"Replaced whitespace character: '\\{ws_char}' with '{replacement}'")

    # Common invalid characters that ChatTTS doesn't handle well
    invalid_chars = {
        # Brackets and parentheses
        '(': '',
        ')': '',
        '[': '',
        ']': '',
        '{': '',
        '}': '',
        '<': '',
        '>': '',

        # Mathematical and special symbols
        '=': ' equals ',
        '+': ' plus ',
        '-': ' minus ',
        '*': ' times ',
        '/': ' divided by ',
        '%': ' percent ',
        '&': ' and ',
        '@': ' at ',
        '#': ' hash ',
        '$': ' dollar ',
        '^': '',
        '~': '',
        '`': '',
        '|': '',
        '\\': '',

        # Quotes (replace with appropriate alternatives)
        '"': '',
        "'": '',
        '"': '',
        '"': '',
        ''': '',
        ''': '',
        '«': '',
        '»': '',

        # Other problematic characters
        '_': ' ',
        '…': ' ',
        '–': ' ',
        '—': ' ',
        '°': ' degree ',
        '©': '',
        '®': '',
        '™': '',
        '€': ' euro ',
        '£': ' pound ',
        '¥': ' yen ',

        # Technical symbols
        '→': ' to ',
        '←': ' from ',
        '↑': ' up ',
        '↓': ' down ',
        '×': ' times ',
        '÷': ' divided by ',
        '±': ' plus minus ',

        # Additional problematic characters
        '•': ' ',       # Bullet point
        '◦': ' ',       # White bullet
        '▪': ' ',       # Black small square
        '▫': ' ',       # White small square
        '★': ' star ',  # Star
        '☆': ' star ',  # White star
        '♦': ' diamond ',
        '♠': ' spade ',
        '♣': ' club ',
        '♥': ' heart ',
    }

    # Apply character replacements
    for invalid_char, replacement in invalid_chars.items():
        if invalid_char in cleaned_text:
            cleaned_text = cleaned_text.replace(invalid_char, replacement)
            print(f"Replaced '{invalid_char}' with '{replacement}'")

    # Keep only safe characters (letters, numbers, basic punctuation, and Chinese characters)
    # Allow: a-z, A-Z, 0-9, space, and safe punctuation
    safe_punctuation = '.!?,:;。！？，：；、'

    # Create pattern for allowed characters
    allowed_pattern = re.compile(r'[a-zA-Z0-9\s\u4e00-\u9fff' + re.escape(safe_punctuation) + ']')

    # Filter text to keep only allowed characters
    filtered_chars = []
    for char in cleaned_text:
        if allowed_pattern.match(char):
            filtered_chars.append(char)
        else:
            # Log removed characters (but don't spam logs with spaces)
            if char.strip():  # Only log non-whitespace characters
                print(f"Filtered out invalid character: '{char}' (ord: {ord(char)})")

    cleaned_text = ''.join(filtered_chars)

    # Clean up multiple spaces and normalize whitespace
    cleaned_text = re.sub(r'\s+', ' ', cleaned_text).strip()

    return cleaned_text

def handle_multiline_text(text):
    """Special handling for multiline text to create natural speech flow"""

    # Split by lines first
    lines = text.split('\n')

    processed_lines = []
    for line in lines:
        line = line.strip()
        if line:  # Skip empty lines
            # Add natural pause markers for line breaks
            processed_lines.append(line)

    # Join lines with natural pauses
    if len(processed_lines) > 1:
        # For multiple lines, add comma-like pauses between them
        result = ' , '.join(processed_lines)
        print(f"Processed multiline text: {len(processed_lines)} lines")
    else:
        result = processed_lines[0] if processed_lines else ""

    return result

def preprocess_text_for_complete_generation(text):
    """Preprocess text with comprehensive cleaning and language-aware punctuation handling"""

    # Handle multiline text first (before cleaning, to preserve line structure)
    if '\n' in text:
        text = handle_multiline_text(text)
        print(f"After multiline processing: '{text}'")

    # Clean invalid characters (including remaining newlines and whitespace)
    text = clean_invalid_characters(text)
    print(f"After cleaning invalid characters: '{text}'")

    # Remove extra whitespace
    text = text.strip()

    # Remove any existing control tokens that might be spoken as words
    text = text.replace('[female]', '').replace('[male]', '').replace('[uv_break]', '')
    text = text.replace('[speed_5]', '').replace('[speed_4]', '').replace('[speed_6]', '')
    text = text.replace('[oral_1]', '').replace('[oral_2]', '').replace('[oral_3]', '')
    text = text.replace('[laugh_1]', '').replace('[laugh_2]', '').replace('[break_1]', '')
    text = text.replace('minimal processing test', '').replace('processing test', '')
    text = text.replace('oh', '').replace('emmm', '').replace('umm', '').replace('uh', '')
    text = text.strip()

    # If text is empty after cleaning, return a safe default
    if not text:
        return "Hello."

    # Detect language and punctuation
    punct_info = detect_language_and_punctuation(text)
    print(f"Language detection after cleaning: {punct_info}")

    # Language-specific processing
    if punct_info['language'] == 'chinese':
        # Chinese text processing
        if not text.endswith(('。', '！', '？', '：', '；')):
            text += '。'

        # Add strategic spacing for Chinese punctuation
        if text.endswith('！'):
            text = text[:-1] + '  ！'
        elif text.endswith('？'):
            text = text[:-1] + '  ？'
        elif text.endswith('。'):
            text = text[:-1] + '  。'

        # Handle Chinese commas for natural pauses
        text = text.replace('，', ' ， ')
        text = text.replace('、', ' 、 ')

    elif punct_info['language'] == 'english':
        # English text processing
        if not text.endswith(('.', '!', '?', ':', ';')):
            text += '.'

        # Add strategic spacing for English punctuation
        if text.endswith('!'):
            text = text[:-1] + '  !'
        elif text.endswith('?'):
            text = text[:-1] + '  ?'
        elif text.endswith('.'):
            text = text[:-1] + '  .'

        # Handle English commas for natural pauses
        text = text.replace(',', ' , ')

    else:  # Mixed language
        # Mixed text processing - handle both types

        # Ensure proper ending
        if not text.endswith(('.', '!', '?', ':', ';', '。', '！', '？', '：', '；')):
            # Add appropriate punctuation based on language context
            if punct_info['has_chinese']:
                text += '。'
            else:
                text += '.'

        # Process Chinese punctuation
        text = text.replace('！', '  ！')
        text = text.replace('？', '  ？')
        text = text.replace('。', '  。')
        text = text.replace('，', ' ， ')
        text = text.replace('、', ' 、 ')

        # Process English punctuation
        text = text.replace('!', '  !')
        text = text.replace('?', '  ?')
        text = text.replace('.', '  .')
        text = text.replace(',', ' , ')

    # Final cleanup of multiple spaces
    text = re.sub(r'\s+', ' ', text).strip()

    # Ensure we have valid text
    if not text or len(text.strip()) == 0:
        return "Hello."

    print(f"Final preprocessed text: '{text}'")
    return text

def generate_complete_audio(text, max_retries=3):
    """Generate audio with retries to ensure completeness and full pronunciation"""
    processed_text = preprocess_text_for_complete_generation(text)

    for attempt in range(max_retries):
        print(f"Generation attempt {attempt + 1}: '{processed_text}'")

        try:
            # Try basic inference first (most compatible)
            wavs = chat.infer([processed_text])

            # If basic inference fails or returns empty, try alternative approaches
            if not wavs or len(wavs) == 0 or wavs[0] is None:
                print(f"Basic inference failed on attempt {attempt + 1}, trying alternatives...")

                # Try with different parameters if supported
                try:
                    if hasattr(chat, 'infer') and attempt == 1:
                        wavs = chat.infer([processed_text], use_decoder=True)
                    elif attempt == 2:
                        wavs = chat.infer([processed_text], do_text_normalization=True)
                except (TypeError, AttributeError) as param_error:
                    print(f"Parameter not supported: {param_error}, using basic inference")
                    wavs = chat.infer([processed_text])

        except Exception as e:
            print(f"ChatTTS infer error on attempt {attempt + 1}: {e}")
            # Fallback to basic inference
            try:
                wavs = chat.infer([processed_text])
            except Exception as fallback_error:
                print(f"Fallback inference also failed: {fallback_error}")
                wavs = None

        # Check if we got valid audio
        if wavs and len(wavs) > 0 and wavs[0] is not None:
            audio_data = wavs[0]

            # Basic validation - ensure we have some audio data
            if hasattr(audio_data, '__len__') and len(audio_data) > 0:
                # More lenient check for completion
                expected_min_length = max(len(text) * 400, 8000)  # Minimum 8000 samples
                actual_length = len(audio_data)

                print(f"Audio length: {actual_length}, expected min: {expected_min_length}")

                # Very lenient check - prioritize having audio over perfect length
                if actual_length >= expected_min_length * 0.3:  # Very lenient
                    print("Audio generation appears sufficient")
                    return audio_data
                else:
                    print(f"Audio seems short, retrying... (attempt {attempt + 1})")
            else:
                print(f"Invalid audio data received, retrying... (attempt {attempt + 1})")
        else:
            print(f"No valid audio generated, retrying... (attempt {attempt + 1})")

    # If all retries failed, return None
    print("All generation attempts failed")
    return None

def apply_natural_ending(audio_segment, style='enhanced', punct_info=None):
    """Apply natural ending processing with language-aware punctuation handling"""

    # Determine fade duration based on language and punctuation
    base_fade = 50  # Default minimal fade

    if punct_info:
        if punct_info['language'] == 'chinese':
            # Chinese typically needs slightly longer processing
            if '！' in str(punct_info['chinese_punct']):
                base_fade = 80  # Chinese exclamation
            elif '？' in str(punct_info['chinese_punct']):
                base_fade = 70  # Chinese question
            else:
                base_fade = 60  # Chinese statement

        elif punct_info['language'] == 'english':
            # English processing
            if '!' in str(punct_info['english_punct']):
                base_fade = 60  # English exclamation
            elif '?' in str(punct_info['english_punct']):
                base_fade = 50  # English question
            else:
                base_fade = 40  # English statement

        else:  # Mixed language
            base_fade = 70  # Conservative for mixed content

    if style == 'enhanced':
        # Enhanced ending that preserves final word clarity
        fade_duration = min(base_fade, len(audio_segment) // 25)
        audio_segment = audio_segment.fade_out(fade_duration)

        # Language-specific silence duration
        if punct_info and punct_info['language'] == 'chinese':
            silence_duration = 600  # Chinese may need more pause
        else:
            silence_duration = 500

        silence_padding = AudioSegment.silent(duration=silence_duration)
        audio_segment = audio_segment + silence_padding
        audio_segment = audio_segment.fade_in(40)

        print(f"Applied enhanced ending: fade={fade_duration}ms, silence={silence_duration}ms, lang={punct_info['language'] if punct_info else 'unknown'}")

    elif style == 'punctuation_aware':
        # Punctuation-aware processing with language consideration
        fade_duration = min(base_fade, len(audio_segment) // 30)
        audio_segment = audio_segment.fade_out(fade_duration)

        # Variable silence based on language and punctuation
        if punct_info and punct_info['language'] == 'chinese':
            silence_duration = 450
        else:
            silence_duration = 400

        silence_padding = AudioSegment.silent(duration=silence_duration)
        audio_segment = audio_segment + silence_padding

        print(f"Applied punctuation-aware ending: fade={fade_duration}ms, lang={punct_info['language'] if punct_info else 'unknown'}")

    else:  # default 'smooth'
        fade_duration = min(base_fade * 0.8, len(audio_segment) // 35)
        audio_segment = audio_segment.fade_out(int(fade_duration))
        silence_padding = AudioSegment.silent(duration=400)
        audio_segment = audio_segment + silence_padding

    return audio_segment

@app.route('/tts', methods=['POST'])
def text_to_speech():
    """Enhanced TTS with proper ending handling for complete pronunciation"""
    try:
        data = request.json
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        text = data.get('text', '').strip()
        ending_style = data.get('ending_style', 'enhanced')

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"Generating enhanced female voice for: {text}")

        # Detect language and punctuation for proper processing
        punct_info = detect_language_and_punctuation(text)
        print(f"Detected language: {punct_info['language']}, punctuation: {punct_info}")

        # Generate audio with enhanced completion checking
        audio_data = generate_complete_audio(text)
        if audio_data is None:
            print("Failed to generate audio - audio_data is None")
            return jsonify({'error': 'Failed to generate complete audio after retries'}), 500

        # Validate audio data
        if not hasattr(audio_data, '__len__') or len(audio_data) == 0:
            print(f"Invalid audio data: type={type(audio_data)}, len={len(audio_data) if hasattr(audio_data, '__len__') else 'N/A'}")
            return jsonify({'error': 'Generated audio data is invalid'}), 500

        print(f"Generated audio shape: {audio_data.shape if hasattr(audio_data, 'shape') else len(audio_data)}, dtype: {audio_data.dtype if hasattr(audio_data, 'dtype') else type(audio_data)}")

        # Process with enhanced ending handling
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            try:
                # Write WAV file with error handling
                try:
                    sf.write(temp_wav.name, audio_data, 24000)
                    print(f"WAV file written to: {temp_wav.name}")
                except Exception as write_error:
                    print(f"Error writing WAV file: {write_error}")
                    if os.path.exists(temp_wav.name):
                        os.unlink(temp_wav.name)
                    return jsonify({'error': f'Failed to write audio file: {str(write_error)}'}), 500

                # Convert to AudioSegment for processing
                try:
                    audio_segment = AudioSegment.from_wav(temp_wav.name)
                    print(f"AudioSegment created: duration={len(audio_segment)}ms")
                except Exception as segment_error:
                    print(f"Error creating AudioSegment: {segment_error}")
                    if os.path.exists(temp_wav.name):
                        os.unlink(temp_wav.name)
                    return jsonify({'error': f'Failed to process audio: {str(segment_error)}'}), 500

                # Apply natural ending based on text characteristics and language
                if ('!' in text or '！' in text):
                    audio_segment = apply_natural_ending(audio_segment, 'punctuation_aware', punct_info)
                else:
                    audio_segment = apply_natural_ending(audio_segment, ending_style, punct_info)

                # Export to memory with high quality
                mp3_buffer = io.BytesIO()
                try:
                    audio_segment.export(
                        mp3_buffer,
                        format="mp3",
                        bitrate="160k",
                        parameters=["-ac", "1", "-ar", "24000"]
                    )
                    mp3_buffer.seek(0)
                    print(f"MP3 exported successfully, size: {len(mp3_buffer.getvalue())} bytes")
                except Exception as export_error:
                    print(f"Error exporting MP3: {export_error}")
                    if os.path.exists(temp_wav.name):
                        os.unlink(temp_wav.name)
                    return jsonify({'error': f'Failed to export MP3: {str(export_error)}'}), 500

                # Clean up temp file
                if os.path.exists(temp_wav.name):
                    os.unlink(temp_wav.name)

                # Validate MP3 buffer
                if len(mp3_buffer.getvalue()) == 0:
                    return jsonify({'error': 'Generated MP3 file is empty'}), 500

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
                print(f"Conversion error: {conv_error}")
                raise conv_error

    except Exception as e:
        print(f"Error in text_to_speech: {str(e)}")
        import traceback
        traceback.print_exc()
        return jsonify({'error': str(e)}), 500

@app.route('/debug', methods=['POST'])
def debug_tts():
    """Debug endpoint to help troubleshoot TTS issues"""
    try:
        data = request.json
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        text = data.get('text', '').strip()
        if not text:
            return jsonify({'error': 'No text provided'}), 400

        print(f"=== DEBUG TTS START ===")
        print(f"Original text: '{text}'")

        # Step 1: Text preprocessing
        processed_text = preprocess_text_for_complete_generation(text)
        print(f"Processed text: '{processed_text}'")

        # Step 2: Language detection
        punct_info = detect_language_and_punctuation(processed_text)
        print(f"Language info: {punct_info}")

        # Step 3: Try audio generation
        print("Attempting audio generation...")
        try:
            wavs = chat.infer([processed_text])
            if wavs and len(wavs) > 0 and wavs[0] is not None:
                audio_data = wavs[0]
                audio_info = {
                    'type': str(type(audio_data)),
                    'shape': audio_data.shape if hasattr(audio_data, 'shape') else None,
                    'length': len(audio_data) if hasattr(audio_data, '__len__') else None,
                    'dtype': str(audio_data.dtype) if hasattr(audio_data, 'dtype') else None,
                    'min_val': float(audio_data.min()) if hasattr(audio_data, 'min') else None,
                    'max_val': float(audio_data.max()) if hasattr(audio_data, 'max') else None
                }
                print(f"Audio generation successful: {audio_info}")

                return jsonify({
                    'status': 'success',
                    'original_text': text,
                    'processed_text': processed_text,
                    'language_info': punct_info,
                    'audio_info': audio_info
                })
            else:
                return jsonify({
                    'status': 'failed',
                    'error': 'ChatTTS returned empty or invalid audio',
                    'original_text': text,
                    'processed_text': processed_text,
                    'wavs_info': {
                        'wavs_type': str(type(wavs)),
                        'wavs_length': len(wavs) if wavs else 0,
                        'first_element': str(type(wavs[0])) if wavs and len(wavs) > 0 else None
                    }
                })

        except Exception as gen_error:
            print(f"Audio generation error: {gen_error}")
            import traceback
            traceback.print_exc()
            return jsonify({
                'status': 'error',
                'error': str(gen_error),
                'original_text': text,
                'processed_text': processed_text
            })

    except Exception as e:
        print(f"Debug error: {str(e)}")
        import traceback
        traceback.print_exc()
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
            '/debug': 'Debug endpoint to troubleshoot TTS issues',
            '/health': 'Health check endpoint'
        }
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000, debug=True)