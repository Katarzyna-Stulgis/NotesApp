package com.example.notesapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.notesapp.R;
import com.example.notesapp.database.NoteViewModel;
import com.example.notesapp.entities.Note;
import com.example.notesapp.listeners.NotesListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    private NoteViewModel noteViewModel;
    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_EDIT_NOTE = 2;

    public static final String IS_FROM_QUICK_ACTIONS = "isFromQuickAction";
    public static final String IS_EDITING = "false";
    public static final String NOTE_URL = "url";
    public static final String IS_NOTE_EXIST = "isViewOrUpdate";
    public static final String NOTE = "note";

    private List<Note> notes;
    private List<Note> notesSource;
    private List<Note> noteList;
    private RecyclerView recyclerView;
    private NoteAdapter adapter;

    private int noteClickedPosition = -1;

    private AlertDialog dialogAddURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.notesRecyclerView);
        adapter = new NoteAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        noteViewModel = ViewModelProviders.of(this).get(NoteViewModel.class);
        noteViewModel.findAll().observe(this, new Observer<List<Note>>() {
            @Override
            public void onChanged(@Nullable final List<Note> notes) {
                adapter.setNotes(notes);
            }
        });

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    notes = noteList;
                } else {
                    adapter.searchNotes(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (notes.size() != 0) {
                    adapter.searchNotes(s.toString());
                }
            }
        });

        findViewById(R.id.imageAddNoteMain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                intent.putExtra(IS_FROM_QUICK_ACTIONS, "false");
                intent.putExtra(IS_EDITING, "false");
                startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
            }
        });

        findViewById(R.id.imageAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                intent.putExtra(IS_FROM_QUICK_ACTIONS, "Add");
                intent.putExtra(IS_EDITING, "false");
                startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
            }
        });

        findViewById(R.id.imageAddWebLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddURLDialog();
            }
        });
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra(IS_NOTE_EXIST, true);
        intent.putExtra(NOTE, (new Gson().toJson(note)));
        intent.putExtra(IS_FROM_QUICK_ACTIONS, "false");
        intent.putExtra(IS_EDITING, "true");
        startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            addNote(data);

            Snackbar.make(findViewById(R.id.mainLayout), getString(R.string.note_added),
                    Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.imageAddNoteMain)
                    .show();
        } else if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode != RESULT_OK) {
            Snackbar.make(findViewById(R.id.mainLayout),
                    getString(R.string.empty_not_added),
                    Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.imageAddNoteMain)
                    .show();
        } else if (requestCode == REQUEST_CODE_EDIT_NOTE && resultCode == RESULT_OK) {
            editNote(data);
        } else if (requestCode == REQUEST_CODE_EDIT_NOTE && resultCode != RESULT_OK && data != null) {
            Snackbar.make(findViewById(R.id.mainLayout),
                    getString(R.string.empty_not_updated),
                    Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.imageAddNoteMain)
                    .show();
        }
    }

    private class NoteHolder extends RecyclerView.ViewHolder {

        private final TextView textTitle;
        private final TextView textSubtitle;
        private final TextView textDateTime;
        LinearLayout layoutNote;
        private Note note;


        public NoteHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textDateTime = itemView.findViewById(R.id.dateTime);
            layoutNote = itemView.findViewById(R.id.layoutNote);
        }

        public void setNote(Note note) {
            textTitle.setText(note.getTitle());
            if (note.getSubtitle().trim().isEmpty()) {
                textTitle.setVisibility(View.GONE);
            } else {
                textSubtitle.setText(note.getSubtitle());
            }
            textDateTime.setText(note.getDateTime());

            GradientDrawable gradientDrawable = (GradientDrawable) layoutNote.getBackground();
            if (note.getColor() != null) {
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }
        }
    }

    private class NoteAdapter extends RecyclerView.Adapter<NoteHolder> {

        private final NotesListener notesListener;

        public NoteAdapter(NotesListener notesListener) {
            this.notesListener = notesListener;
        }

        @NonNull
        @Override
        public NoteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new NoteHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.item_container_note,
                            parent,
                            false
                    )
            );
        }

        @Override
        public void onBindViewHolder(@NonNull NoteHolder holder, int position) {
            if (notes != null) {
                Note note = notes.get(position);
                holder.setNote(note);
                int tempPosition = position;
                holder.layoutNote.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        notesListener.onNoteClicked(note, tempPosition);
                    }
                });
            } else {
                Log.d("MainActivity", "No notes");
            }
        }

        @Override
        public int getItemCount() {
            if (notes != null) {
                return notes.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        void setNotes(List<Note> notesList) {
            notes = notesList;
            notesSource = new LinkedList<>(notesList);
            noteList = new LinkedList<>(notesList);
            notifyDataSetChanged();
        }

        void searchNotes(final String searchKeyWord) {
            if (searchKeyWord.trim().isEmpty()) {
                notes = notesSource;
            } else {
                LinkedList<Note> temp = new LinkedList<>();
                for (Note note : notesSource) {
                    if (note.getTitle().toLowerCase().contains(searchKeyWord.toLowerCase())
                            || note.getSubtitle().toLowerCase().contains(searchKeyWord.toLowerCase())
                            || note.getNoteText().toLowerCase().contains(searchKeyWord.toLowerCase())) {
                        temp.add(note);
                    }
                }
                notes = temp;
            }
            notifyDataSetChanged();
        }
    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
                        Toast.makeText(MainActivity.this, R.string.enter_url, Toast.LENGTH_SHORT).show();
                    } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                        Toast.makeText(MainActivity.this, R.string.enter_valid_url, Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        dialogAddURL.dismiss();
                        intent.putExtra(IS_FROM_QUICK_ACTIONS, "Url");
                        intent.putExtra(IS_EDITING, "false");
                        intent.putExtra(NOTE_URL, inputURL.getText().toString());
                        inputURL.setText("");
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    inputURL.setText("");
                    dialogAddURL.dismiss();
                }
            });
        }
        dialogAddURL.show();
    }

    private void addNote(Intent data) {
        Note note = new Note();
        note.setTitle(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_TITLE));
        note.setSubtitle(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_SUBTITLE));
        note.setNoteText(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_TEXT));
        note.setDateTime(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_DATE));
        note.setColor(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_COLOR));
        note.setWebLink(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_URL));
        noteViewModel.insert(note);

        notes.add(note);
        adapter.notifyItemInserted(notes.size());
        recyclerView.smoothScrollToPosition(notes.size());
    }

    private void editNote(Intent data) {
        Note editNote = noteViewModel.findAll().getValue().get(noteClickedPosition);

        String isNoteRemoved = data.getStringExtra(CreateNoteActivity.IS_NOTE_DELETED);

        if (isNoteRemoved.equals("true")) {
            noteViewModel.delete(editNote);
            notes.remove(noteClickedPosition);
            adapter.notifyItemRemoved(noteClickedPosition);

            Snackbar.make(findViewById(R.id.mainLayout), getString(R.string.note_deleted),
                    Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.imageAddNoteMain)
                    .show();
        } else {
            editNote.setTitle(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_TITLE));
            editNote.setSubtitle(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_SUBTITLE));
            editNote.setNoteText(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_TEXT));
            editNote.setDateTime(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_DATE));
            editNote.setColor(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_COLOR));
            editNote.setWebLink(data.getStringExtra(CreateNoteActivity.EXTRA_NOTE_URL));
            noteViewModel.update(editNote);
            notes.remove(noteClickedPosition);
            notes.add(noteClickedPosition, noteList.get(noteClickedPosition));
            adapter.notifyItemChanged(noteClickedPosition);

            Snackbar.make(findViewById(R.id.mainLayout), getString(R.string.note_updated),
                    Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.imageAddNoteMain)
                    .show();
        }
    }
}