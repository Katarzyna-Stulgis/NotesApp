package com.example.notesapp.activities;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.notesapp.R;
import com.example.notesapp.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_TITLE = "com.example.PROJEKT.NOTE_TITLE";
    public static final String EXTRA_NOTE_SUBTITLE = "com.example.PROJEKT.NOTE_SUBTITLE";
    public static final String EXTRA_NOTE_TEXT = "com.example.PROJEKT.NOTE_TEXT";
    public static final String EXTRA_NOTE_DATE = "com.example.PROJEKT.NOTE_DATE";
    public static final String EXTRA_NOTE_COLOR = "com.example.PROJEKT.NOTE_COLOR";
    public static final String EXTRA_NOTE_URL = "com.example.PROJEKT.NOTE_URL";
    public static final String EXTRA_NOTE_ID = "com.example.PROJEKT.NOTE_ID";
    public static final String IS_NOTE_DELETED = "isNoteDeleted";

    public static final String NOTE_COLOR_1 = "#333333";
    public static final String NOTE_COLOR_2 = "#2196F3";
    public static final String NOTE_COLOR_3 = "#009688";
    public static final String NOTE_COLOR_4 = "#E91E63";
    public static final String NOTE_COLOR_5 = "#673AB7";

    private static final int PERMISSION_REQUEST_CODE = 200;

    private View viewSubtitleIndicator;
    private EditText inputNoteTitle;
    private EditText inputNoteSubtitle;
    private EditText inputNoteText;
    private TextView textDateTime;
    private TextView textWebURL;
    private LinearLayout layoutWebURL;

    private String selectedNoteColor;

    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteNote;

    private Note existingNote;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_create_note);

        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent replyIntent = new Intent();
                replyIntent.putExtra(EXTRA_NOTE_TITLE, "true");
                onBackPressed();
            }
        });

        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inputNoteText = findViewById(R.id.inputNote);
        textDateTime = findViewById(R.id.textDateTime);
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        textWebURL = findViewById(R.id.textWebURL);
        layoutWebURL = findViewById(R.id.layoutWebURL);
        textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );

        if (getIntent().getStringExtra(MainActivity.IS_FROM_QUICK_ACTIONS).equals("Url")
                && !getIntent().getStringExtra(MainActivity.IS_EDITING).equals("true")) {
            textWebURL.setText(getIntent().getStringExtra(MainActivity.NOTE_URL));
            layoutWebURL.setVisibility(View.VISIBLE);
        }

        ImageView imageSave = findViewById(R.id.imageSave);
        imageSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        if (getIntent().getBooleanExtra(MainActivity.IS_NOTE_EXIST, false)) {
            String s = (String) getIntent().getSerializableExtra(MainActivity.NOTE);
            existingNote = new Gson().fromJson(s, Note.class);
            setViewEditNote();
        }

        findViewById(R.id.imageRemoveWebUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textWebURL.setText(null);
                layoutWebURL.setVisibility(View.GONE);
            }
        });

        selectedNoteColor = NOTE_COLOR_1;
        setSubtitleIndicatorColor();
        initPickNote();
    }

    private void saveNote() {
        Intent replyIntent = new Intent();
        if (TextUtils.isEmpty(inputNoteTitle.getText())
                || TextUtils.isEmpty(inputNoteSubtitle.getText())
                || TextUtils.isEmpty(inputNoteText.getText())) {
            setResult(RESULT_CANCELED, replyIntent);
        } else {
            String noteTitle = inputNoteTitle.getText().toString();
            replyIntent.putExtra(EXTRA_NOTE_TITLE, noteTitle);
            String noteSubtitle = inputNoteSubtitle.getText().toString();
            replyIntent.putExtra(EXTRA_NOTE_SUBTITLE, noteSubtitle);
            String noteText = inputNoteText.getText().toString();
            replyIntent.putExtra(EXTRA_NOTE_TEXT, noteText);
            String noteDate = textDateTime.getText().toString();
            replyIntent.putExtra(EXTRA_NOTE_DATE, noteDate);
            String noteColor = selectedNoteColor;
            replyIntent.putExtra(EXTRA_NOTE_COLOR, noteColor);

            if (layoutWebURL.getVisibility() == View.VISIBLE) {
                String noteURL = textWebURL.getText().toString();
                replyIntent.putExtra(EXTRA_NOTE_URL, noteURL);
            }

            if (getIntent().getStringExtra(MainActivity.IS_FROM_QUICK_ACTIONS).equals("Url")
                    && getIntent().getStringExtra(MainActivity.IS_EDITING).equals("false")) {
                textWebURL.setText(getIntent().getStringExtra(MainActivity.NOTE_URL));
                layoutWebURL.setVisibility(View.VISIBLE);
            } else if (getIntent().getExtras() != null
                    && getIntent().getStringExtra(MainActivity.IS_EDITING).equals("true")) {
                String noteID = String.valueOf(existingNote.getId());
                replyIntent.putExtra(EXTRA_NOTE_ID, noteID);
                replyIntent.putExtra(IS_NOTE_DELETED, "false");
            }
            setResult(RESULT_OK, replyIntent);
        }
        finish();
        selectedNoteColor = NOTE_COLOR_1;
        setSubtitleIndicatorColor();
    }

    private void setViewEditNote() {
        inputNoteTitle.setText(existingNote.getTitle());
        inputNoteSubtitle.setText(existingNote.getSubtitle());
        inputNoteText.setText(existingNote.getNoteText());
        textDateTime.setText(existingNote.getDateTime());
        if (existingNote.getWebLink() != null && !existingNote.getWebLink().trim().isEmpty()) {
            textWebURL.setText(existingNote.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);
        }
    }

    private void initPickNote() {
        final LinearLayout layout = findViewById(R.id.layoutPickNoteColor);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layout);
        layout.findViewById(R.id.textPickNoteColor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        final ImageView imageColor1 = layout.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = layout.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = layout.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = layout.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = layout.findViewById(R.id.imageColor5);

        layout.findViewById(R.id.viewColor1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = NOTE_COLOR_1;
                imageColor1.setImageResource(R.drawable.ic_done);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);

                setSubtitleIndicatorColor();
            }
        });

        layout.findViewById(R.id.viewColor2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = NOTE_COLOR_2;
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(R.drawable.ic_done);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);

                setSubtitleIndicatorColor();
            }
        });

        layout.findViewById(R.id.viewColor3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = NOTE_COLOR_3;
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(R.drawable.ic_done);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);

                setSubtitleIndicatorColor();
            }
        });

        layout.findViewById(R.id.viewColor4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = NOTE_COLOR_4;
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(R.drawable.ic_done);
                imageColor5.setImageResource(0);

                setSubtitleIndicatorColor();
            }
        });

        layout.findViewById(R.id.viewColor5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = NOTE_COLOR_5;
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(R.drawable.ic_done);

                setSubtitleIndicatorColor();
            }
        });

        if (existingNote != null && existingNote.getColor() != null && !existingNote.getColor().trim().isEmpty()) {
            switch (existingNote.getColor()) {
                case NOTE_COLOR_2:
                    layout.findViewById(R.id.viewColor2).performClick();
                    break;
                case NOTE_COLOR_3:
                    layout.findViewById(R.id.viewColor3).performClick();
                    break;
                case NOTE_COLOR_4:
                    layout.findViewById(R.id.viewColor4).performClick();
                    break;
                case NOTE_COLOR_5:
                    layout.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }

        layout.findViewById(R.id.layoutAddUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddURLDialog();
            }
        });

        if (existingNote != null) {
            layout.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    showDeleteNoteDialog();
                }
            });

            layout.findViewById(R.id.layoutSaveAsPDF).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.layoutSaveAsPDF).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    saveNoteAsPDF();
                }
            });
        }
    }

    private void saveNoteAsPDF() {

        if (!checkPermission()) {
            requestPermission();
        }

        PdfDocument pdfDocument = new PdfDocument();

        Paint page = new Paint();

        String noteTitle = inputNoteTitle.getText().toString();
        String noteSubtitle = inputNoteSubtitle.getText().toString();
        String noteText = inputNoteText.getText().toString();
        String noteDate = textDateTime.getText().toString();

        int pageHeight = 1120;
        int pageWidth = 792;
        PdfDocument.PageInfo mypageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();

        PdfDocument.Page myPage = pdfDocument.startPage(mypageInfo);

        Canvas canvas = myPage.getCanvas();
        page.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        page.setTextSize(15);

        canvas.drawText(getString(R.string.title) + " " + noteTitle, 80, 80, page);
        canvas.drawText(getString(R.string.subtitle) + " " + noteSubtitle, 80, 120, page);
        canvas.drawText(getString(R.string.date) + " " + noteDate, 80, 140, page);
        if (layoutWebURL.getVisibility() == View.VISIBLE) {
            String noteURL = textWebURL.getText().toString();
            canvas.drawText(getString(R.string.url) + " " + noteURL, 80, 160, page);
        }
        canvas.drawText(noteText, 80, 220, page);

        page.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        page.setTextSize(15);

        pdfDocument.finishPage(myPage); //adding atributes to pdf


        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File fileDir = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(fileDir, noteTitle + ".pdf");

        try {
            pdfDocument.writeTo(new FileOutputStream(file));

            Toast.makeText(this, R.string.pdf_success, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pdfDocument.close();
    }

    private boolean checkPermission() {
        int permission = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean writeStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                if (writeStorage) {
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private void showDeleteNoteDialog() {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    findViewById(R.id.layoutDeleteNoteContainer)
            );
            builder.setView(view);

            dialogDeleteNote = builder.create();

            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent();
                    replyIntent.putExtra(IS_NOTE_DELETED, "true");
                    setResult(RESULT_OK, replyIntent);
                    selectedNoteColor = NOTE_COLOR_1;
                    setSubtitleIndicatorColor();
                    finish();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogDeleteNote.dismiss();
                }
            });

        }
        dialogDeleteNote.show();
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    findViewById(R.id.layoutAddUrlContainer)
            );
            builder.setView(view);

            dialogAddURL = builder.create();

            if (dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (inputURL.getText().toString().trim().isEmpty()) {
                        Toast.makeText(CreateNoteActivity.this, R.string.enter_url, Toast.LENGTH_SHORT).show();
                    } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                        Toast.makeText(CreateNoteActivity.this, R.string.enter_valid_url, Toast.LENGTH_SHORT).show();
                    } else {
                        textWebURL.setText(inputURL.getText().toString());
                        layoutWebURL.setVisibility(View.VISIBLE);
                        dialogAddURL.dismiss();
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogAddURL.dismiss();
                }
            });
        }
        dialogAddURL.show();
    }
}