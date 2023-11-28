
# Verifie Android Client


[![](https://jitpack.io/v/verifie-global/verifie-android-client.svg)](https://jitpack.io/#verifie-global/verifie-android-client/v2.7.3)

# Install

**Step 1.** Add the JitPack repository to your project level build.gradle file

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

**Step 2.** Add the dependency to your application(module) level build.gradle file

```gradle
implementation 'com.github.verifie-global:verifie-android-client:v2.7.4'
```

# Usage

```java
VerifieColorConfig colorConfig = new VerifieColorConfig();  
colorConfig.setDocCropperFrameColor(Color.GREEN);

VerifieTextConfig textConfig = new VerifieTextConfig();  
textConfig.setMovePhoneCloser("Move phone closer");  
...

VerifieConfig config = new VerifieConfig("licenseKey", "personId", docType);
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

        verifie.setIdCardView(new IDCardView() {
            @Override
            public View getViewToShow(ActionHandler actionHandler) {
            Return the view you want to add after ID card first page scanning, use action handler to close the layout and remove the view you have added
                someBtnOnYourView.setOnClickListener(v -> actionHandler.closeIDCardLayout());
                    return yourView;
                    return null;
            }
        });
        verifie.setFaceContainingPercentageInOval(0.5f);
```
 Added new features, now you can set how many percent the face should be in the oval to allow the face scanner work, alse you can add a view between ID Card First page and Second Page scaning
```

verifie.start();
```

You can also provide you own fragments for document scanner and face detector screen.

```java
config.setDocumentScannerFragment(YourDocumentScannerFragment.class);
config.setFaceDetectorFragment(YourFaceDetectorFragment.class);
```

```YourDocumentScannerFragment``` and ```YourFaceDetectorFragment``` should extend ```com.verifie.android.ui.BaseDocumentScannerFragment```
