package com.briatka.pavol.bookworm;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MyDialogFragment extends DialogFragment {

    @BindView(R.id.enter_title_edit_text)
    EditText enterTitleEditText;
    @BindView(R.id.ok_button)
    TextView okButton;
    @BindView(R.id.cancel_button)
    TextView cancelButton;

    OnInputListener listener;

    public interface OnInputListener {
        void sendInput(String input);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.fragment_dialog, container, false);
        ButterKnife.bind(this,mainView);

        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = enterTitleEditText.getText().toString();
                if(!TextUtils.isEmpty(input)) {
                    listener.sendInput(input);
                    getDialog().dismiss();
                } else {
                    Toast.makeText(getContext(),
                            getString(R.string.book_title_hint),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        return mainView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnInputListener) getActivity();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }
}
