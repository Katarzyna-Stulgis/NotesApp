package com.example.notesapp.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.notesapp.dao.NoteDao;
import com.example.notesapp.entities.Note;

import java.util.List;

public class NoteRepository {
    private final NoteDao noteDao;
    private final LiveData<List<Note>> notes;

    NoteRepository(Application application) {
        NotesDatabase database = NotesDatabase.getDatabase(application);
        noteDao = database.noteDao();
        notes = noteDao.getAllNotes();
    }

    LiveData<List<Note>> findAllNotes() {
        return notes;
    }

    void insert(Note note) {
        NotesDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.insertNote(note);
        });
    }

    void update(Note note) {
        NotesDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.updateNote(note);
        });
    }

    void delete(Note note) {
        NotesDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.deleteNote(note);
        });
    }
}
