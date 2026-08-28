from flask import Flask, request, jsonify
from PIL import Image
from transformers import BlipProcessor, BlipForConditionalGeneration
import requests
from io import BytesIO

app = Flask(__name__)

print("Loading BLIP model... (this happens once at startup)")
processor = BlipProcessor.from_pretrained("Salesforce/blip-image-captioning-base")
model = BlipForConditionalGeneration.from_pretrained("Salesforce/blip-image-captioning-base")
print("Model loaded. Server ready.")


def generate_caption(raw_image):
    inputs = processor(raw_image, return_tensors="pt")
    out = model.generate(**inputs, max_new_tokens=30)
    return processor.decode(out[0], skip_special_tokens=True)


def load_image_from_url(img_url):
    response = requests.get(img_url, stream=True, timeout=10)
    response.raise_for_status()
    return Image.open(BytesIO(response.content)).convert("RGB")


# GET - pass an image URL as a query param
@app.route("/caption", methods=["GET"])
def caption_from_url_get():
    img_url = request.args.get("url")

    if not img_url:
        return jsonify({"error": "Missing 'url' query parameter"}), 400

    try:
        raw_image = load_image_from_url(img_url)
    except Exception as e:
        return jsonify({"error": f"Could not load image: {str(e)}"}), 400

    try:
        description = generate_caption(raw_image)
    except Exception as e:
        return jsonify({"error": f"Captioning failed: {str(e)}"}), 500

    return jsonify({"url": img_url, "description": description})


# POST - upload an image file directly
@app.route("/caption", methods=["POST"])
def caption_from_upload():
    if "image" not in request.files:
        return jsonify({"error": "No file uploaded. Use form field name 'image'"}), 400

    file = request.files["image"]

    if file.filename == "":
        return jsonify({"error": "Empty filename"}), 400

    try:
        raw_image = Image.open(file.stream).convert("RGB")
    except Exception as e:
        return jsonify({"error": f"Could not read image: {str(e)}"}), 400

    try:
        description = generate_caption(raw_image)
    except Exception as e:
        return jsonify({"error": f"Captioning failed: {str(e)}"}), 500

    return jsonify({"filename": file.filename, "description": description})


# POST - pass an image URL in a JSON body
@app.route("/caption-url", methods=["POST"])
def caption_from_url_post():
    data = request.get_json(silent=True)

    if not data or "url" not in data:
        return jsonify({"error": "Missing 'url' in JSON body"}), 400

    img_url = data["url"]

    try:
        raw_image = load_image_from_url(img_url)
    except Exception as e:
        return jsonify({"error": f"Could not load image: {str(e)}"}), 400

    try:
        description = generate_caption(raw_image)
    except Exception as e:
        return jsonify({"error": f"Captioning failed: {str(e)}"}), 500

    return jsonify({"url": img_url, "description": description})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)