package com.droids.tamada.filemanager.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.droids.tamada.filemanager.Animations.AVLoadingIndicatorView;
import com.droids.tamada.filemanager.activity.FullImageViewActivity;
import com.droids.tamada.filemanager.activity.ImageViewActivity;
import com.droids.tamada.filemanager.activity.MainActivity;
import com.droids.tamada.filemanager.activity.TextFileViewActivity;
import com.droids.tamada.filemanager.adapter.InternalStorageListAdapter;
import com.droids.tamada.filemanager.app.AppController;
import com.droids.tamada.filemanager.helper.PreferManager;
import com.droids.tamada.filemanager.model.InternalStorageFilesModel;
import com.example.satish.filemanager.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class InternalStorageFragment extends Fragment implements MainActivity.ButtonBackPressListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private RecyclerView recyclerView;
    private LinearLayout noMediaLayout;
    private OnFragmentInteractionListener mListener;
    private ArrayList<InternalStorageFilesModel> internalStorageFilesModelArrayList;
    private InternalStorageListAdapter internalStorageListAdapter;
    private String rootPath;
    private RelativeLayout footerLayout;
    private TextView lblFilePath;
    private ArrayList<String> arrayListFilePaths;
    private PreferManager preferManager;
    private int selectedFilePosition;
    private final HashMap selectedFileHashMap = new HashMap();
    private boolean isCheckboxVisible = false;
    private AVLoadingIndicatorView progressBar;

    public InternalStorageFragment() {
        // Required empty public constructor
    }

    public static InternalStorageFragment newInstance(String param1, String param2) {
        InternalStorageFragment fragment = new InternalStorageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_internal_storage, container, false);
        AppController.getInstance().setButtonBackPressed(this);
        preferManager = new PreferManager(AppController.getInstance().getApplicationContext());
        progressBar = (AVLoadingIndicatorView) view.findViewById(R.id.progressBar);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        noMediaLayout = (LinearLayout) view.findViewById(R.id.noMediaLayout);
        footerLayout = (RelativeLayout) view.findViewById(R.id.id_layout_footer);
        lblFilePath = (TextView) view.findViewById(R.id.id_file_path);
        ImageView imgDelete = (ImageView) view.findViewById(R.id.id_delete);
        final ImageView imgFileCopy = (ImageView) view.findViewById(R.id.id_copy_file);
        ImageView imgMenu = (ImageView) view.findViewById(R.id.id_menu);
        internalStorageFilesModelArrayList = new ArrayList<>();
        arrayListFilePaths = new ArrayList<>();
        rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        internalStorageListAdapter = new InternalStorageListAdapter(internalStorageFilesModelArrayList);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(AppController.getInstance().getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(internalStorageListAdapter);
        arrayListFilePaths.add(rootPath);
        getFilesList(rootPath);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(AppController.getInstance().getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                InternalStorageFilesModel internalStorageFilesModel = internalStorageFilesModelArrayList.get(position);
                if (internalStorageFilesModel.isCheckboxVisible()) {//if list item selected
                    if (internalStorageFilesModel.isSelected()) {
                        internalStorageFilesModel.setSelected(false);
                        internalStorageFilesModelArrayList.remove(position);
                        internalStorageFilesModelArrayList.add(position, internalStorageFilesModel);
                        internalStorageListAdapter.notifyDataSetChanged();
                        selectedFileHashMap.remove(position);
                    } else {
                        selectedFileHashMap.put(position, internalStorageFilesModel.getFilePath());
                        internalStorageFilesModel.setSelected(true);
                        selectedFilePosition = position;
                        internalStorageFilesModelArrayList.remove(position);
                        internalStorageFilesModelArrayList.add(position, internalStorageFilesModel);
                        internalStorageListAdapter.notifyDataSetChanged();
                    }
                } else {
                    File file = new File(internalStorageFilesModel.getFilePath());//get the selected item path
                    openFile(file, internalStorageFilesModel);
                }
                if (selectedFileHashMap.isEmpty()) {
                    if (footerLayout.getVisibility() != View.GONE) {
                        Animation topToBottom = AnimationUtils.loadAnimation(AppController.getInstance().getApplicationContext(),
                                R.anim.top_bottom);
                        footerLayout.startAnimation(topToBottom);
                        footerLayout.setVisibility(View.GONE);
                    }
                } else {
                    if (footerLayout.getVisibility() != View.VISIBLE) {
                        Animation bottomToTop = AnimationUtils.loadAnimation(AppController.getInstance().getApplicationContext(),
                                R.anim.bottom_top);
                        footerLayout.startAnimation(bottomToTop);
                        footerLayout.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                if (footerLayout.getVisibility() != View.VISIBLE) {
                    Animation bottomToTop = AnimationUtils.loadAnimation(AppController.getInstance().getApplicationContext(),
                            R.anim.bottom_top);
                    footerLayout.startAnimation(bottomToTop);
                    footerLayout.setVisibility(View.VISIBLE);
                }
                for (int i = 0; i < internalStorageFilesModelArrayList.size(); i++) {
                    InternalStorageFilesModel internalStorageFilesModel = internalStorageFilesModelArrayList.get(i);
                    internalStorageFilesModel.setCheckboxVisible(true);
                    isCheckboxVisible = true;
                    if (position == i) {
                        internalStorageFilesModel.setSelected(true);
                        selectedFileHashMap.put(position, internalStorageFilesModel.getFilePath());
                        selectedFilePosition = position;
                    }
                }
                internalStorageListAdapter.notifyDataSetChanged();
            }
        }));

        imgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteFile();
            }
        });

        imgMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMenu();
            }
        });

        imgFileCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                footerLayout.setVisibility(View.GONE);
                for (int i = 0; i < internalStorageFilesModelArrayList.size(); i++) {
                    InternalStorageFilesModel internalStorageFilesModel = internalStorageFilesModelArrayList.get(i);
                    internalStorageFilesModel.setCheckboxVisible(false);
                }
                internalStorageListAdapter.notifyDataSetChanged();
                isCheckboxVisible = false;
            }
        });

        return view;
    }

    @Override
    public void onButtonBackPressed(int navItemIndex) {
        if (selectedFileHashMap.size() == 0)
            if (footerLayout.getVisibility() != View.GONE) {
                Animation topToBottom = AnimationUtils.loadAnimation(AppController.getInstance().getApplicationContext(),
                        R.anim.top_bottom);
                footerLayout.startAnimation(topToBottom);
                footerLayout.setVisibility(View.GONE);
            } else {
                if (isCheckboxVisible) {
                    for (int i = 0; i < internalStorageFilesModelArrayList.size(); i++) {
                        InternalStorageFilesModel internalStorageFilesModel = internalStorageFilesModelArrayList.get(i);
                        internalStorageFilesModel.setCheckboxVisible(false);
                    }
                    internalStorageListAdapter.notifyDataSetChanged();
                    isCheckboxVisible = false;
                } else {
                    if (navItemIndex == 0) {
                        if (arrayListFilePaths.size() == 1) {
                            Toast.makeText(AppController.getInstance().getApplicationContext(), "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
                        }
                        if (arrayListFilePaths.size() != 0) {
                            if (arrayListFilePaths.size() >= 2) {
                                internalStorageFilesModelArrayList.clear();
                                getFilesList(arrayListFilePaths.get(arrayListFilePaths.size() - 2));
                                internalStorageListAdapter.notifyDataSetChanged();
                            }
                            arrayListFilePaths.remove(arrayListFilePaths.size() - 1);
                        } else {
                            getActivity().finish();
                            System.exit(0);
                        }
                    }
                }
            }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void deleteFile() {
        final Dialog dialogDeleteFile = new Dialog(getActivity(), android.R.style.Theme_Translucent_NoTitleBar);
        dialogDeleteFile.setContentView(R.layout.custom_delete_file_dialog);
        dialogDeleteFile.show();
        Button btnOkay = (Button) dialogDeleteFile.findViewById(R.id.btn_okay);
        Button btnCancel = (Button) dialogDeleteFile.findViewById(R.id.btn_cancel);
        TextView lblDeleteFile = (TextView) dialogDeleteFile.findViewById(R.id.id_lbl_delete_files);
        if (selectedFileHashMap.size() == 1) {
            lblDeleteFile.setText(AppController.getInstance().getApplicationContext().getResources().getString(R.string.lbl_delete_single_file));
        } else {
            lblDeleteFile.setText(AppController.getInstance().getApplicationContext().getResources().getString(R.string.lbl_delete_multiple_files));
        }
        btnOkay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Set set = selectedFileHashMap.keySet();
                    Iterator itr = set.iterator();
                    while (itr.hasNext()) {
                        int i = Integer.parseInt(itr.next().toString());
                        File deleteFile = new File((String) selectedFileHashMap.get(i));//create file for selected file
                        boolean isDeleteFile = deleteFile.delete();//delete the file from memory
                        if (isDeleteFile) {
                            selectedFileHashMap.remove(i);
                            InternalStorageFilesModel model = internalStorageFilesModelArrayList.get(i);
                            internalStorageFilesModelArrayList.remove(model);//remove file from listview
                            internalStorageListAdapter.notifyDataSetChanged();//refresh the adapter
                            selectedFileHashMap.remove(selectedFilePosition);
                        }
                    }
                    dialogDeleteFile.dismiss();
                    footerLayout.setVisibility(View.GONE);
                } catch (Exception e) {
                    AppController.getInstance().trackException(e);
                    e.printStackTrace();
                }
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDeleteFile.dismiss();
            }
        });
    }

    private void openFile(File file, InternalStorageFilesModel internalStorageFilesModel) {
        if (file.isDirectory()) {//check if selected item is directory
            if (file.canRead()) {//if directory is readable
                internalStorageFilesModelArrayList.clear();
                arrayListFilePaths.add(internalStorageFilesModel.getFilePath());
                getFilesList(internalStorageFilesModel.getFilePath());
                internalStorageListAdapter.notifyDataSetChanged();
            } else {//Toast to your not openable type
                Toast.makeText(AppController.getInstance().getApplicationContext(), "Folder can't be read!", Toast.LENGTH_SHORT).show();
            }
            //if file is not directory open a application for file type
        }
    }

    private void getFilesList(String filePath) {
        rootPath = filePath;
        lblFilePath.setText(filePath);
        File f = new File(filePath);
        File[] files = f.listFiles();
        if (files != null) {
            if (files.length == 0) {
                noMediaLayout.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                noMediaLayout.setVisibility(View.GONE);
            }
            for (File file : files) {
                InternalStorageFilesModel model = new InternalStorageFilesModel();
                model.setFileName(file.getName());
                model.setFilePath(file.getPath());
                model.setCheckboxVisible(false);
                model.setSelected(false);
                if (file.isDirectory()) {
                    model.setDir(true);
                } else {
                    model.setDir(false);
                }

                if (!preferManager.isHiddenFileVisible()) {
                    if (file.getName().indexOf('.') != 0) {
                        internalStorageFilesModelArrayList.add(model);
                    }
                } else { //display hidden files
                    internalStorageFilesModelArrayList.add(model);
                }
            }
        }
    }

    private void showMenu() {
        final Dialog menuDialog = new Dialog(getActivity(), android.R.style.Theme_Translucent_NoTitleBar);
        menuDialog.setContentView(R.layout.custom_menu_dialog);
        TextView lblRenameFile = (TextView) menuDialog.findViewById(R.id.id_rename);
        TextView lblFileMove = (TextView) menuDialog.findViewById(R.id.id_move);

        if (selectedFileHashMap.size() == 1) {
            lblRenameFile.setClickable(true);
            lblRenameFile.setFocusable(true);
            lblFileMove.setClickable(true);
            lblFileMove.setFocusable(true);
            lblRenameFile.setTextColor(ContextCompat.getColor(AppController.getInstance().getApplicationContext(), R.color.color_text_selected));
            lblFileMove.setTextColor(ContextCompat.getColor(AppController.getInstance().getApplicationContext(), R.color.color_text_selected));
        } else {
            lblRenameFile.setClickable(false);
            lblRenameFile.setFocusable(false);
            lblFileMove.setClickable(false);
            lblFileMove.setFocusable(false);
            lblRenameFile.setTextColor(ContextCompat.getColor(AppController.getInstance().getApplicationContext(), R.color.color_text_unselected));
            lblFileMove.setTextColor(ContextCompat.getColor(AppController.getInstance().getApplicationContext(), R.color.color_text_unselected));
        }

        lblFileMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuDialog.dismiss();
                footerLayout.setVisibility(View.GONE);
                for (int i = 0; i < internalStorageFilesModelArrayList.size(); i++) {
                    InternalStorageFilesModel internalStorageFilesModel = internalStorageFilesModelArrayList.get(i);
                    internalStorageFilesModel.setCheckboxVisible(false);
                }
                internalStorageListAdapter.notifyDataSetChanged();
                isCheckboxVisible = false;
            }
        });
        lblRenameFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InternalStorageFilesModel internalStorageFilesModel = internalStorageFilesModelArrayList.get(selectedFilePosition);
                renameFile(menuDialog, internalStorageFilesModel.getFileName(), internalStorageFilesModel.getFilePath(), selectedFilePosition);
            }
        });
        menuDialog.show();
    }

    private void moveFile(String outputPath) {
        progressBar.setVisibility(View.VISIBLE);
        try {
            Set set = selectedFileHashMap.keySet();
            Iterator itr = set.iterator();
            while (itr.hasNext()) {
                int i = Integer.parseInt(itr.next().toString());
                File file = new File((String) selectedFileHashMap.get(i));
                InputStream in = null;
                OutputStream out = null;
                try {
                    //create output directory if it doesn't exist
                    File dir = new File(outputPath);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    in = new FileInputStream((String) selectedFileHashMap.get(i));
                    out = new FileOutputStream(outputPath + "/" + file.getName());
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    in = null;
                    // write the output file
                    out.flush();
                    out.close();
                    out = null;
                    // delete the original file
                    new File((String) selectedFileHashMap.get(i)).delete();
                } catch (Exception e) {
                    AppController.getInstance().trackException(e);
                    Log.e("tag", e.getMessage());
                }
                InternalStorageFilesModel model = new InternalStorageFilesModel();
                model.setSelected(false);
                model.setFilePath(outputPath + "/" + file.getName());
                model.setFileName(file.getName());
                if (new File(outputPath + "/" + file.getName()).isDirectory()) {
                    model.setIsDir(true);
                } else {
                    model.setIsDir(false);
                }
                internalStorageFilesModelArrayList.add(model);
            }
            internalStorageListAdapter.notifyDataSetChanged();//refresh the adapter
            selectedFileHashMap.clear();
            footerLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            AppController.getInstance().trackException(e);
            e.printStackTrace();
            Toast.makeText(AppController.getInstance().getApplicationContext(), "unable to process this action", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }

    }

    private void copyFile(String outputPath) {
        progressBar.setVisibility(View.VISIBLE);
        try {
            Set set = selectedFileHashMap.keySet();
            Iterator itr = set.iterator();
            while (itr.hasNext()) {
                int i = Integer.parseInt(itr.next().toString());
                File file = new File((String) selectedFileHashMap.get(i));
                InputStream in = new FileInputStream((String) selectedFileHashMap.get(i));
                OutputStream out = new FileOutputStream(outputPath + "/" + file.getName());
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                InternalStorageFilesModel model = new InternalStorageFilesModel();
                model.setSelected(false);
                model.setFilePath(outputPath + "/" + file.getName());
                model.setFileName(file.getName());
                if (new File(outputPath + "/" + file.getName()).isDirectory()) {
                    model.setIsDir(true);
                } else {
                    model.setIsDir(false);
                }
                internalStorageFilesModelArrayList.add(model);
            }
            internalStorageListAdapter.notifyDataSetChanged();//refresh the adapter
            selectedFileHashMap.clear();
            footerLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            AppController.getInstance().trackException(e);
            e.printStackTrace();
            Toast.makeText(AppController.getInstance().getApplicationContext(), "unable to process this action", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void renameFile(final Dialog menuDialog, String fileName, final String filePath, final int selectedFilePosition) {
        final Dialog dialogRenameFile = new Dialog(getActivity(), android.R.style.Theme_Translucent_NoTitleBar);
        dialogRenameFile.setContentView(R.layout.custom_rename_file_dialog);
        dialogRenameFile.show();
        final EditText txtRenameFile = (EditText) dialogRenameFile.findViewById(R.id.txt_file_name);
        Button btnRename = (Button) dialogRenameFile.findViewById(R.id.btn_rename);
        Button btnCancel = (Button) dialogRenameFile.findViewById(R.id.btn_cancel);
        txtRenameFile.setText(fileName);
        btnRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (txtRenameFile.getText().toString().trim().length() == 0) {
                    Toast.makeText(AppController.getInstance().getApplicationContext(), "Please enter file name", Toast.LENGTH_SHORT).show();
                } else {
                    File renamedFile = new File(filePath.substring(0, filePath.lastIndexOf('/') + 1) + txtRenameFile.getText().toString());
                    if (renamedFile.exists()) {
                        Toast.makeText(AppController.getInstance().getApplicationContext(), "File already exits,choose another name", Toast.LENGTH_SHORT).show();
                    } else {
                        final File oldFile = new File(filePath);//create file with old name
                        boolean isRenamed = oldFile.renameTo(renamedFile);
                        if (isRenamed) {
                            InternalStorageFilesModel model = internalStorageFilesModelArrayList.get(selectedFilePosition);
                            model.setFileName(txtRenameFile.getText().toString());
                            model.setFilePath(renamedFile.getPath());
                            if (renamedFile.isDirectory()) {
                                model.setIsDir(true);
                            } else {
                                model.setIsDir(false);
                            }
                            model.setSelected(false);
                            internalStorageFilesModelArrayList.remove(selectedFilePosition);
                            internalStorageFilesModelArrayList.add(selectedFilePosition, model);
                            internalStorageListAdapter.notifyDataSetChanged();
                            dialogRenameFile.dismiss();
                            menuDialog.dismiss();
                            footerLayout.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(AppController.getInstance().getApplicationContext(), AppController.getInstance().getApplicationContext().getString(R.string.msg_prompt_not_renamed), Toast.LENGTH_SHORT).show();
                            dialogRenameFile.dismiss();
                            menuDialog.dismiss();
                            footerLayout.setVisibility(View.GONE);
                        }
                    }
                }
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtRenameFile.setText("");
                dialogRenameFile.dismiss();
            }
        });
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {
        private GestureDetector gestureDetector;
        private ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

}
