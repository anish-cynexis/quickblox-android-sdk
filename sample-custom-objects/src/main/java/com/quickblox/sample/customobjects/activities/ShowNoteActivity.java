package com.quickblox.sample.customobjects.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.customobjects.QBCustomObjects;
import com.quickblox.customobjects.model.QBCustomObject;
import com.quickblox.sample.core.utils.ResourceUtils;
import com.quickblox.sample.core.utils.Toaster;
import com.quickblox.sample.customobjects.R;
import com.quickblox.sample.customobjects.helper.DataHolder;
import com.quickblox.sample.customobjects.model.Note;

import java.util.HashMap;
import java.util.List;

import static com.quickblox.sample.customobjects.definition.Consts.CLASS_NAME;
import static com.quickblox.sample.customobjects.definition.Consts.COMMENTS;
import static com.quickblox.sample.customobjects.definition.Consts.STATUS;
import static com.quickblox.sample.customobjects.definition.Consts.STATUS_DONE;
import static com.quickblox.sample.customobjects.definition.Consts.STATUS_IN_PROCESS;
import static com.quickblox.sample.customobjects.definition.Consts.STATUS_NEW;

public class ShowNoteActivity extends BaseActivity {

    private static final String EXTRA_POSITION = "position";

    private TextView titleTextView;
    private TextView statusTextView;
    private TextView commentsTextView;
    private int position;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        initUI();
        fillFields();
    }

    private void initUI() {
        actionBar.setDisplayHomeAsUpEnabled(true);
        position = getIntent().getIntExtra(EXTRA_POSITION, 0);
        titleTextView = (TextView) findViewById(R.id.note_textview);
        statusTextView = (TextView) findViewById(R.id.status_textview);
        commentsTextView = (TextView) findViewById(R.id.comments_textview);
    }

    private void fillFields() {
        titleTextView.setText(DataHolder.getDataHolder().getNoteTitle(position));
        statusTextView.setText(DataHolder.getDataHolder().getNoteStatus(position));
        applyComment();
    }

    private void applyComment() {
        List<String> notes = DataHolder.getDataHolder().getNoteComments(position);
        String commentsStr = null;
        for (int i = 0; i < notes.size(); ++i) {
            commentsStr += "#" + i + "-" + notes.get(i) + "\n\n";
        }
        commentsTextView.setText(commentsStr);
    }

    private void setNewNote(QBCustomObject qbCustomObject) {
        Note note = new Note(qbCustomObject);
        DataHolder.getDataHolder().setNoteToNoteList(position, note);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_comment_button:
                showAddNewCommentDialog();
                break;
            case R.id.change_status_button:
                showSetNewStatusDialog();
                break;
            case R.id.delete_button:
                progressDialog.show();

                // Delete note
                QBCustomObjects.deleteObject(CLASS_NAME, DataHolder.getDataHolder().getNoteId(position), new QBEntityCallbackImpl<String>() {
                    @Override
                    public void onSuccess() {
                        progressDialog.dismiss();

                        DataHolder.getDataHolder().removeNoteFromList(position);
                        Toaster.longToast(R.string.note_successfully_deleted);
                        finish();
                    }

                    @Override
                    public void onError(List<String> errors) {
                        Toaster.longToast(errors.get(0));

                        progressDialog.dismiss();
                    }
                });

                break;
        }
    }

    private void showAddNewCommentDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.new_comment);
        alert.setMessage(R.string.write_new_comment);

        final EditText editText = new EditText(this);
        editText.setTextColor(ResourceUtils.getColor(R.color.white));
        alert.setView(editText);
        editText.setSingleLine();
        alert.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonNumber) {
                        progressDialog.show();
                        addNewComment(editText.getText().toString());
                        dialog.cancel();
                    }
                }
        );

        alert.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                }
        );
        alert.show();
    }

    private void showSetNewStatusDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final String[] statusList = {STATUS_NEW, STATUS_IN_PROCESS, STATUS_DONE};
        alert.setTitle(R.string.choose_new_status);

        alert.setItems(statusList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                progressDialog.show();
                String status;
                if (item == 0) {
                    status = STATUS_NEW;
                } else if (item == 1) {
                    status = STATUS_IN_PROCESS;
                } else {
                    status = STATUS_DONE;
                }
                updateNoteStatus(status);
            }
        });
        alert.show();
    }

    private void addNewComment(String comment) {
        DataHolder.getDataHolder().addNewComment(position, comment);
        // create query for update activity_note status
        // set class name
        // add new comments
        HashMap<String, Object> fields = new HashMap<String, Object>();
        fields.put(COMMENTS, DataHolder.getDataHolder().getComments(position));
        QBCustomObject qbCustomObject = new QBCustomObject();
        qbCustomObject.setCustomObjectId(DataHolder.getDataHolder().getNoteId(position));
        qbCustomObject.setClassName(CLASS_NAME);
        qbCustomObject.setFields(fields);

        QBCustomObjects.updateObject(qbCustomObject, new QBEntityCallbackImpl<QBCustomObject>() {
            @Override
            public void onSuccess(QBCustomObject qbCustomObject, Bundle bundle) {
                progressDialog.dismiss();

                setNewNote(qbCustomObject);
                applyComment();
            }

            @Override
            public void onError(List<String> errors) {
                progressDialog.dismiss();
                Toaster.longToast(errors.get(0));
            }
        });
    }

    private void updateNoteStatus(String status) {
        HashMap<String, Object> fields = new HashMap<String, Object>();
        fields.put(STATUS, status);
        QBCustomObject qbCustomObject = new QBCustomObject();
        qbCustomObject.setCustomObjectId(DataHolder.getDataHolder().getNoteId(position));
        qbCustomObject.setClassName(CLASS_NAME);
        qbCustomObject.setFields(fields);

        QBCustomObjects.updateObject(qbCustomObject, new QBEntityCallbackImpl<QBCustomObject>() {
            @Override
            public void onSuccess(QBCustomObject qbCustomObject, Bundle bundle) {
                progressDialog.dismiss();

                setNewNote(qbCustomObject);
                statusTextView.setText(DataHolder.getDataHolder().getNoteStatus(position));
            }

            @Override
            public void onError(List<String> errors) {
                progressDialog.dismiss();

                Toaster.longToast(errors.get(0));
            }
        });
    }
}