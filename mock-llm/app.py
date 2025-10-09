from flask import Flask, jsonify, request
from waitress import serve

app = Flask(__name__)


@app.route('/api/llm/process/email', methods=['POST'])
def process_email():
    return jsonify({"output": "llm output"})


@app.route('/api/llm/process/pv', methods=['POST'])
def process_pv():
    return jsonify({"output": "llm output"})


@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "UP", "service": "mock-llm"})


if __name__ == '__main__':
    serve(app, host='0.0.0.0', port=8081)


