# Save as minimal_enhanced_server.py
# This version keeps your exact working logic but adds minimal load balancing
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
import threading
from concurrent.futures import ThreadPoolExecutor
import time
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

class MinimalEnhancedChatTTS:
    def __init__(self, max_workers=2):
        self.max_workers = max_workers
        self.executor = ThreadPoolExecutor(max_workers=max_workers)
        self.chat_instances = []
        self.current_instance = 0
        self.lock = threading.Lock()
        self.stats = {
            'total_requests': 0,
            'concurrent_requests': 0
        }

        # Initialize ChatTTS instances
        logger.info(f"Initializing {max_workers} ChatTTS instances...")
        for i in range(max_workers):
            logger.info(f"Loading ChatTTS instance {i+1}/{max_workers}...")
            chat = ChatTTS.Chat()
            chat.load(compile=False)

            if hasattr(chat, 'config'):
                chat.config.prompt = '[speed_5]'

            self.chat_instances.append(chat)

        logger.info("All ChatTTS instances loaded successfully!")

    def get_chat_instance(self):
        """Round-robin load balancing across ChatTTS instances"""
        with self.lock:
            instance = self.chat_instances[self.current_instance]
            self.current_instance = (self.current_instance + 1) % len(self.chat_instances)
            return instance

# Global TTS engine
tts_engine = None

def initialize_tts():
    """Initialize TTS engine with minimal enhancements"""
    global tts_engine
    tts_engine = MinimalEnhancedChatTTS(max_workers=2)

def preprocess_text_for_complete_generation(text):
    """Preprocess text to ensure ChatTTS generates complete audio (EXACT COPY)"""
    text = text.strip()
    text = '[female] ' + text

    if not text.endswith(('.', '!', '?', ':', ';')):
        text += '.'

    text += ' [uv_break]'
    return text

def generate_complete_audio(text, max_retries=3):
    """Generate audio with load balancing only (NO CACHING to avoid errors)"""
    processed_text = preprocess_text_for_complete_generation(text)

    # Get a ChatTTS instance (load balanced)
    chat_instance = tts_engine.get_chat_instance()

    for attempt in range(max_retries):
        logger.info(f"Generation attempt {attempt + 1}: '{processed_text}'")

        try:
            wavs = chat_instance.infer(
                [processed_text],
                use_decoder=True,
                do_text_normalization=True,
            )
        except Exception as e:
            logger.warning(f"ChatTTS infer error: {e}")
            wavs = chat_instance.infer([processed_text])

        if wavs and len(wavs) > 0:
            audio_data = wavs[0]

            # Simple completeness check
            if len(audio_data) > 1000:
                logger.info("Audio generation appears complete")
                tts_engine.stats['total_requests'] += 1
                return audio_data
            else:
                logger.warning(f"Audio seems short, retrying... (attempt {attempt + 1})")
        else:
            logger.warning(f"No audio generated, retrying... (attempt {attempt + 1})")

    # If all retries failed, use the last generation
    logger.warning("Using final generation attempt")
    if wavs and len(wavs) > 0:
        tts_engine.stats['total_requests'] += 1
        return wavs[0]
    return None

@app.route('/tts', methods=['POST'])
def text_to_speech():
    """Simple, reliable text to speech conversion with load balancing"""
    try:
        start_time = time.time()
        data = request.json
        text = data.get('text', '').strip()
        fade_out_ms = data.get('fade_out_ms', 300)
        silence_ms = data.get('silence_ms', 200)

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        logger.info(f"Processing TTS request: {text[:50]}...")

        # Generate audio with load balancing (no caching)
        audio_data = generate_complete_audio(text)
        if audio_data is None:
            return jsonify({'error': 'Failed to generate complete audio after retries'}), 500

        logger.info(f"Generated audio shape: {audio_data.shape}, dtype: {audio_data.dtype}")

        # EXACT COPY of your working MP3 conversion logic
        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            try:
                sf.write(temp_wav.name, audio_data, 24000)
                logger.info(f"WAV file written to: {temp_wav.name}")

                audio_segment = AudioSegment.from_wav(temp_wav.name)
                fade_duration = min(fade_out_ms, len(audio_segment) // 4)
                audio_segment = audio_segment.fade_out(fade_duration)
                silence_padding = AudioSegment.silent(duration=silence_ms)
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

                total_time = time.time() - start_time
                logger.info(f"MP3 generated successfully, size: {len(mp3_buffer.getvalue())} bytes")
                logger.info(f"Total request time: {total_time:.2f}s")

                response = send_file(
                    mp3_buffer,
                    download_name='generated_audio.mp3',
                    mimetype='audio/mpeg',
                    as_attachment=True
                )
                response.headers['X-Generation-Time'] = f"{total_time:.2f}"
                response.headers['X-Workers'] = str(tts_engine.max_workers)

                return response

            except Exception as conv_error:
                if os.path.exists(temp_wav.name):
                    os.unlink(temp_wav.name)
                raise conv_error

    except Exception as e:
        logger.error(f"Error in TTS generation: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/tts/batch', methods=['POST'])
def text_to_speech_batch():
    """Process multiple texts concurrently"""
    try:
        data = request.json
        texts = data.get('texts', [])

        if not texts or not isinstance(texts, list):
            return jsonify({'error': 'No texts array provided'}), 400

        if len(texts) > 3:  # Conservative limit
            return jsonify({'error': 'Too many texts (max 3 per batch)'}), 400

        start_time = time.time()

        # Process texts using the thread pool
        futures = []
        for text in texts:
            if len(text.strip()) > 500:  # Conservative limit
                continue
            future = tts_engine.executor.submit(generate_complete_audio, text.strip())
            futures.append((text, future))

        # Collect results
        results = []
        for text, future in futures:
            try:
                audio_data = future.result(timeout=120)
                if audio_data is not None:
                    # Convert using working logic
                    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
                        sf.write(temp_wav.name, audio_data, 24000)
                        audio_segment = AudioSegment.from_wav(temp_wav.name)

                        fade_duration = min(300, len(audio_segment) // 4)
                        audio_segment = audio_segment.fade_out(fade_duration)
                        silence_padding = AudioSegment.silent(duration=200)
                        audio_segment = audio_segment + silence_padding

                        mp3_buffer = io.BytesIO()
                        audio_segment.export(mp3_buffer, format="mp3", bitrate="128k")

                        os.unlink(temp_wav.name)

                        mp3_data = base64.b64encode(mp3_buffer.getvalue()).decode()

                        results.append({
                            'text': text,
                            'audio_base64': mp3_data,
                            'format': 'mp3',
                            'success': True
                        })
                else:
                    results.append({
                        'text': text,
                        'error': 'Failed to generate audio',
                        'success': False
                    })
            except Exception as e:
                results.append({
                    'text': text,
                    'error': str(e),
                    'success': False
                })

        total_time = time.time() - start_time

        return jsonify({
            'results': results,
            'total_time': total_time,
            'processed_count': len(results),
            'success_count': len([r for r in results if r.get('success', False)])
        })

    except Exception as e:
        logger.error(f"Error in batch TTS: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/stats', methods=['GET'])
def get_stats():
    """Get basic statistics"""
    if tts_engine:
        return jsonify({
            'total_requests': tts_engine.stats['total_requests'],
            'workers': tts_engine.max_workers,
            'caching': 'disabled',
            'load_balancing': 'enabled'
        })
    return jsonify({'error': 'TTS engine not initialized'}), 500

@app.route('/test', methods=['GET'])
def test_audio():
    """Test endpoint (EXACT COPY of working version)"""
    try:
        text = "This is a test audio file."

        audio_data = generate_complete_audio(text)
        if audio_data is None:
            return jsonify({'error': 'Failed to generate complete test audio'}), 500

        with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_wav:
            sf.write(temp_wav.name, audio_data, 24000)
            audio_segment = AudioSegment.from_wav(temp_wav.name)

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
                download_name='test_audio.mp3',
                mimetype='audio/mpeg',
                as_attachment=True
            )

    except Exception as e:
        logger.error(f"Test error: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """Health check"""
    return jsonify({
        'status': 'healthy',
        'service': 'Minimal Enhanced ChatTTS API',
        'features': {
            'load_balancing': True,
            'caching': False,  # Disabled to avoid array errors
            'batch_processing': True,
            'complete_generation': True,
            'smooth_endings': True
        },
        'workers': tts_engine.max_workers if tts_engine else 0
    })

# Initialize TTS on startup
logger.info("Starting Minimal Enhanced ChatTTS Server...")
initialize_tts()
logger.info("Server initialization complete!")

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000, debug=True, threaded=True)