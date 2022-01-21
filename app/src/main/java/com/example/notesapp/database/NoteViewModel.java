package com.example.notesapp.database;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.notesapp.entities.Note;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {

    private final NoteRepository noteRepository;

    private final LiveData<List<Note>> notes;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        noteRepository = new NoteRepository(application);
        notes = noteRepository.findAllNotes();
    }

    public LiveData<List<Note>> findAll() {
        return notes;
    }

    public void insert(Note note) {
        noteRepository.insert(note);
    }

    public void update(Note book) {
        noteRepository.update(book);
    }

    public void delete(Note book) {
        noteRepository.delete(book);
    }
}
