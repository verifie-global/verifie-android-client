
# Verifie Android Client

[![](https://jitpack.io/v/verifie-global/verifie-android.svg)](https://jitpack.io/#verifie-global/verifie-android)

# Install

**Step 1.** Add the JitPack repository to your build file

```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
	
	aaptOptions {
        	noCompress "tflite"
    	}
}
```

**Step 2.** Add the dependency

```gradle
implementation 'com.github.verifie-global:verifie-android-client:v1.0.5'
```

# Usage

```java
VerifieColorConfig colorConfig = new VerifieColorConfig();  
colorConfig.setDocCropperFrameColor(Color.GREEN);

VerifieTextConfig textConfig = new VerifieTextConfig();  
textConfig.setMovePhoneCloser("Move phone closer");  
...

VerifieConfig config = new VerifieConfig("licenseKey", "personId");
config.setColorConfig(colorConfig);
config.setTextConfig(textConfig);

Verifie verifie = new Verifie(this, config, new VerifieCallback() {  
      
    @Override  
	public void onSessionStarted() {
		// Verifie session started
    }
  
    @Override  
    public void onDocumentReceived(Document document) {  
		// Document scan result received
    }
    
    @Override  
    public void onScoreReceived(Score score) {
	    // Facial score received
    }
});
verifie.start();
```

You can also provide you own fragments for document scanner and face detector screen.

```java
config.setDocumentScannerFragment(YourDocumentScannerFragment.class);
config.setFaceDetectorFragment(YourFaceDetectorFragment.class);
```

```YourDocumentScannerFragment``` and ```YourFaceDetectorFragment``` should extend ```com.verifie.android.ui.BaseDocumentScannerFragment```
