import cv2
from utils.dataset import CocoDataset
import torch
from utils.model_utils import InferFasterRCNN

def detect_flower_stage(data):
    """Çiçek aşamasını tespit eder."""
    image_data = data['image']  # Görüntü yolu

    # Görüntü yolunu tanımla
    image_path = r"image_to_inference/received_image.png"  # Gelen veriden görüntüyü oku (gönderildiği dosya adı)

    # Görüntü boyutlarını tanımla
    height, width = 640, 640
    classnames = ['BUD', 'FLOWER', 'OVERFLOWER']  # Sınıflar
    num_classes = len(classnames) + 1

    # Cihazı tanımla
    device = torch.device("cpu")

    # Modeli yükle
    IF_C = InferFasterRCNN(num_classes=num_classes, classnames=classnames)
    IF_C.load_model(checkpoint=r'src/main/resources/plant_stage_detection/flower_stage_model.pth', device=device)

    # Görüntüyü dönüştür
    transform_info = CocoDataset.transform_image_for_inference(image_path, width=width, height=height)
    result = IF_C.infer_image(transform_info=transform_info, visualize=False)

    # Eşik değerlerini tanımla
    thresholds = {
        'BUD': 0.90,
        'FLOWER': 0.55,
        'OVERFLOWER': 0.50
    }

    # Filtrelenmiş sonuçları topla
    filtered_results = []
    class_counts = {classname: 0 for classname in classnames}  # Her sınıf için sayımı başlat

    # Görüntüyü yükle
    image = cv2.imread(image_path)

    for class_name, score, box in zip(result['pred_classes'], result['scores'], result['scaled_boxes']):
        if score >= thresholds[class_name]:
            filtered_results.append((class_name, score, box))
            class_counts[class_name] += 1

            # Kutuyu çiz
            x1, y1, x2, y2 = map(int, box)  # Koordinatları tam sayıya dönüştür
            cv2.rectangle(image, (x1, y1), (x2, y2), (0, 255, 0), 2)  # Yeşil kare çiz
            cv2.putText(image, f'{class_name}: {score:.2f}', (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

    # Sonucu kaydetme
    output_path = "output/output_image.png"
    cv2.imwrite(output_path, image)

    # Toplam nesne sayısı ve sınıf başına tespit sayısını yazdırma
    total_objects = sum(class_counts.values())

    # Sonuçları geri döndür
    return {
        'class_counts': class_counts,
        'total_objects': total_objects,
        'output_image': output_path
    }

def flower_stage_handler(data):
    image_path = data.get('image_path')  # Gönderilen görüntü yolu
    if not image_path:
        return {"error": "No image path provided"}

    # Çiçek aşamalarını tespit et
    result = detect_flower_stage({'image': image_path})

    # Sonuçları döndür
    return result