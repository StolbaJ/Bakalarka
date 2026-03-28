@app.route('/machine/execute', methods=['POST'])
def execute_task():
    data = request.json
    print(f"Brousím lyže ID: {data['skiId']} na úhel {data['angle']}°")
    return jsonify({"status": "started"}), 202