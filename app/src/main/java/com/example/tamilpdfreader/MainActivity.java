package com.example.tamilpdfreader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.github.barteksc.pdfviewer.PDFView;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import java.io.File;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_PDF_REQUEST = 1;
    private PDFView pdfView;
    private TextView extractedText, speedLabel;
    private Button selectPdfButton, readTextButton;
    private SeekBar speedSeekBar;
    private TextToSpeech textToSpeech;
    private String extractedTamilText = "";
    private float speechSpeed = 1.0f;
    private String[] words;
    private int wordIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pdfView = findViewById(R.id.pdfView);
        extractedText = findViewById(R.id.textView);
        selectPdfButton = findViewById(R.id.selectPdfButton);
        readTextButton = findViewById(R.id.readTextButton);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        speedLabel = findViewById(R.id.speedLabel);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("ta", "IN"));
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        runOnUiThread(() -> highlightNextWord());
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> extractedText.setText(extractedTamilText));
                    }

                    @Override
                    public void onError(String utteranceId) {}
                });
            }
        });

        selectPdfButton.setOnClickListener(v -> pickPdfFile());
        readTextButton.setOnClickListener(v -> readTamilText());

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speechSpeed = 0.5f + (progress / 10.0f);
                textToSpeech.setSpeechRate(speechSpeed);
                speedLabel.setText("Speech Speed: " + speechSpeed + "x");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void pickPdfFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(intent, PICK_PDF_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                File file = new File(data.getData().getPath());
                pdfView.fromUri(data.getData()).load();
                extractTextFromPdf(file);
            } catch (Exception e) {
                Toast.makeText(this, "Error loading PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void extractTextFromPdf(File file) {
        try {
            PdfReader reader = new PdfReader(file.getAbsolutePath());
            PdfDocument pdfDocument = new PdfDocument(reader);
            StringBuilder text = new StringBuilder();

            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                text.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))).append(" ");
            }

            extractedTamilText = text.toString();
            extractedText.setText(extractedTamilText);
            words = extractedTamilText.split(" ");
            wordIndex = 0;
            reader.close();
        } catch (Exception e) {
            Toast.makeText(this, "Error extracting text", Toast.LENGTH_SHORT).show();
        }
    }

    private void readTamilText() {
        if (extractedTamilText.isEmpty()) {
            Toast.makeText(this, "No text to read!", Toast.LENGTH_SHORT).show();
            return;
        }
        textToSpeech.speak(extractedTamilText, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE");
    }

    private void highlightNextWord() {
        if (wordIndex < words.length) {
            StringBuilder highlightedText = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i == wordIndex) {
                    highlightedText.append("<font color='#FF0000'><b>").append(words[i]).append("</b></font> ");
                } else {
                    highlightedText.append(words[i]).append(" ");
                }
            }
            extractedText.setText(android.text.Html.fromHtml(highlightedText.toString()));
            wordIndex++;
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

