---
sidebar_position: 1
slug: /
description: Android Camera Parameters डैशबोर्ड और इसकी प्रमुख नैदानिक (diagnostic) विशेषताओं का अवलोकन, जिसमें हार्डवेयर लेवल डिटेक्शन और रियल-टाइम फीचर ट्रैकिंग शामिल है।
keywords: [android camera dashboard, hardware level detection, camera diagnostics, हिंदी]
---

# ऐप अवलोकन (App Overview)

यह पेज एप्लिकेशन के डैशबोर्ड और प्रमुख विशेषताओं का विस्तृत विवरण प्रदान करता है।

![App Overview](/img/camera_params_feature_graph.png)

## डैशबोर्ड घटक (Dashboard Components)

### 1. नेविगेशन और चयन (Navigation & Selection)
- **मेन्यू ड्रॉर (Menu Drawer)**: ऊपर-बाएँ मेन्यू आइकन के माध्यम से गोपनीयता नीति (Privacy Policy), ऐप को रेट करें (Rate App), और ऐप के बारे में (About) जानकारी प्राप्त करें।
- **कैमरा चयन (Camera Selection)**: उपलब्ध लेंसों (Rear, Front, Ultra-wide, आदि) के बीच स्विच करने के लिए कैमरा नाम या आईडी बैज (जैसे, "0") पर टैप करें।
- **बॉटम नेविगेशन (Bottom Navigation)**: **Overview**, **Categories**, **Raw JSON**, और **Favorites** के बीच आसानी से स्विच करें।

### 2. सारांश कार्ड (Summary Card)
शीर्ष पर स्थित सारांश कार्ड सबसे महत्वपूर्ण जानकारी प्रदान करता है:
- **हार्डवेयर लेवल (Hardware Level)**: Camera2 API सपोर्ट लेवल (LEGACY, LIMITED, FULL, या LEVEL_3)। यह लेंस की समग्र क्षमताओं को निर्धारित करता है।

### 3. प्रमुख विशेषताओं का ग्रिड (Key Features Grid)
पेशेवर-स्तर की विशेषताओं के लिए तत्काल स्थिति प्रदान करने वाला एक दृश्य ग्रिड:
- **रेज़ोल्यूशन और सेंसर साइज (Resolution & Sensor Size)**: सेंसर की भौतिक विशेषताएं।
- **मैक्स वीडियो FPS (Max Video FPS)**: पीक फ्रेम रेट क्षमताएं।
- **RAW सपोर्ट**: इंगित करता है कि क्या सेंसर असम्पीडित (uncompressed) डेटा आउटपुट कर सकता है।
- **OIS (Optical Image Stabilization)**: भौतिक लेंस स्थिरीकरण की उपलब्धता।
- **मैनुअल कंट्रोल (Manual Control)**: मैनुअल एक्सपोज़र और मैनुअल फोकस सपोर्ट की स्थिति।
- **प्रोसेसिंग (Processing)**: HDR, फेस डिटेक्शन, और रेड-आई रिडक्शन के लिए सपोर्ट।

### 4. श्रेणीबद्ध पैरामीटर (Categorized Parameters - Categories Tab)
तार्किक समूहों में व्यवस्थित CameraCharacteristics की पूरी सूची देखें:
- **सेंसर (Sensor)**: रेज़ोल्यूशन, भौतिक आकार, संवेदनशीलता (sensitivity) रेंज।
- **लेंस (Lens)**: फोकल लेंथ, एपर्चर, स्टेबलाइजेशन मोड।
- **AE/AF/AWB**: एक्सपोज़र, फोकस और व्हाइट बैलेंस के लिए विस्तृत कंट्रोल मोड।
- **सर्च (Search)**: विशिष्ट API कुंजियों या मानों को जल्दी से खोजने के लिए एकीकृत सर्च बार का उपयोग करें।
