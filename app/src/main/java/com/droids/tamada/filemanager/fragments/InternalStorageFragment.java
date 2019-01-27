package com.droids.tamada.filemanager.fragments;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.droids.tamada.filemanager.Animations.AVLoadingIndicatorView;
import com.droids.tamada.filemanager.Utils.Utils;
import com.droids.tamada.filemanager.activity.MainActivity;
import com.droids.tamada.filemanager.adapter.InternalStorageListAdapter;
import com.droids.tamada.filemanager.app.AppController;
import com.droids.tamada.filemanager.helper.PreferManager;
import com.droids.tamada.filemanager.model.InternalStorageFilesModel;
import com.example.satish.filemanager.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


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
    private final HashMap<Integer, String> selectedFileHashMap = new HashMap();
    private boolean isCheckboxVisible = false;
    private AVLoadingIndicatorView progressBar;

    private boolean isMusicFolder = false;
    private boolean isMovieFolder = false;

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
                        String uriString = null;
                        if (internalStorageFilesModel.getIsDir()) {
                            uriString = Uri.fromFile(new File(internalStorageFilesModel.getFilePath())).toString();
                        } else {
                            if (isMusicFolder) {
                                uriString = getUriString(internalStorageFilesModel.getFilePath(), true);
                            } else if (isMovieFolder) {
                                uriString = getUriString(internalStorageFilesModel.getFilePath(), false);
                            }
                        }
                        Log.d("mytest", "onclick uriString = " + uriString);
                        selectedFileHashMap.put(position, uriString);
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
                        String uriString = null;
                        if (internalStorageFilesModel.getIsDir()) {
                            uriString = Uri.fromFile(new File(internalStorageFilesModel.getFilePath())).toString();
                        } else {
                            if (isMusicFolder) {
                                uriString = getUriString(internalStorageFilesModel.getFilePath(), true);
                            } else if (isMovieFolder) {
                                uriString = getUriString(internalStorageFilesModel.getFilePath(), false);
                            }
                        }
                        Log.d("mytest", "onlongclick uriString = " + uriString);
                        selectedFileHashMap.put(position, uriString);
                        selectedFilePosition = position;
                    }
                }
                internalStorageListAdapter.notifyDataSetChanged();
            }
        }));

        imgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                footerLayout.setVisibility(View.GONE);
                for (int i = 0; i < internalStorageFilesModelArrayList.size(); i++) {
                    InternalStorageFilesModel internalStorageFilesModel = internalStorageFilesModelArrayList.get(i);
                    internalStorageFilesModel.setCheckboxVisible(false);
                    internalStorageFilesModel.setSelected(false);
                }
                internalStorageListAdapter.notifyDataSetChanged();
                isCheckboxVisible = false;

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
                    internalStorageFilesModel.setSelected(false);
                }
                internalStorageListAdapter.notifyDataSetChanged();
                isCheckboxVisible = false;

                copy();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.parrot.filemanager.action.DELETE_RESULT");
        filter.addAction("com.parrot.filemanager.action.RENAME_RESULT");
        AppController.getInstance().getApplicationContext().registerReceiver(mReceiver, filter);

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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.parrot.filemanager.action.DELETE_RESULT".equals(action)) {
                Log.d("mytest", "Refresh list after delete");
                internalStorageFilesModelArrayList.clear();
                getFilesList(rootPath);
                internalStorageListAdapter.notifyDataSetChanged();
            } else if ("com.parrot.filemanager.action.RENAME_RESULT".equals(action)) {
                Log.d("mytest", "Refresh list after rename");
                internalStorageFilesModelArrayList.clear();
                getFilesList(rootPath);
                internalStorageListAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        AppController.getInstance().getApplicationContext().unregisterReceiver(mReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            Log.d("mytest", "Refresh list after move");
            internalStorageFilesModelArrayList.clear();
            getFilesList(rootPath);
            internalStorageListAdapter.notifyDataSetChanged();
        }
    }

    private void deleteFile() {
        String[] uriStrings = Utils.getUriStringForHashmap(selectedFileHashMap);
        Intent intent = new Intent("com.parrot.filemanager.action.DELETE");
        intent.setPackage("com.parrot.car.filemanager");
        intent.putExtra("file_uri", uriStrings);
        if (isMusicFolder) {
            intent.putExtra("app_type", "Music");
        } else if (isMovieFolder) {
            intent.putExtra("app_type", "MTT");
        }
        AppController.getInstance().getApplicationContext().startService(intent);
    }

    private void openFile(File file, InternalStorageFilesModel internalStorageFilesModel) {
        if (file.isDirectory()) {//check if selected item is directory
            if (file.canRead()) {//if directory is readable
                internalStorageFilesModelArrayList.clear();
                arrayListFilePaths.add(internalStorageFilesModel.getFilePath());
                String path = internalStorageFilesModel.getFilePath().substring(20);
                Log.d("mytest", "path = " + path);
                if (path.startsWith("Music")) {
                    isMusicFolder = true;
                    isMovieFolder = false;
                } else if (path.startsWith("Movies")) {
                    isMusicFolder = false;
                    isMovieFolder = true;
                } else {
                    isMusicFolder = false;
                    isMovieFolder = false;
                }
                Log.d("mytest", "isMusic = " + isMusicFolder + " isMovies = " + isMovieFolder);
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
                if (filePath.equals(Environment.getExternalStorageDirectory().getAbsolutePath())
                        && !"Music".equals(file.getName()) && !"Movies".equals(file.getName())) {
                    continue;
                }
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
                    internalStorageFilesModel.setSelected(false);
                }
                internalStorageListAdapter.notifyDataSetChanged();
                isCheckboxVisible = false;

                move();
            }
        });
        lblRenameFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuDialog.dismiss();
                footerLayout.setVisibility(View.GONE);
                for (int i = 0; i < internalStorageFilesModelArrayList.size(); i++) {
                    InternalStorageFilesModel internalStorageFilesModel = internalStorageFilesModelArrayList.get(i);
                    internalStorageFilesModel.setCheckboxVisible(false);
                    internalStorageFilesModel.setSelected(false);
                }
                internalStorageListAdapter.notifyDataSetChanged();
                isCheckboxVisible = false;

                rename();
            }
        });
        menuDialog.show();
    }

    private void move() {
        String[] uriStrings = Utils.getUriStringForHashmap(selectedFileHashMap);
        Intent intent = new Intent("com.parrot.filemanager.action.PASTE");
        intent.addCategory("com.parrot.filemanager.CATEGORY");
        intent.setComponent(new ComponentName("com.parrot.car.filemanager",
                "com.parrot.car.filemanager.FileManagerActivity"));
        intent.putExtra("file_uri", uriStrings);
        if (isMusicFolder) {
            intent.putExtra("app_type", "Music");
        } else if (isMovieFolder) {
            intent.putExtra("app_type", "MTT");
        }
        intent.putExtra("is_move", true);
        startActivityForResult(intent, 0);
        selectedFileHashMap.clear();
    }

    private void copy() {
        String[] uriStrings = Utils.getUriStringForHashmap(selectedFileHashMap);
        Intent intent = new Intent("com.parrot.filemanager.action.PASTE");
        intent.addCategory("com.parrot.filemanager.CATEGORY");
        intent.setComponent(new ComponentName("com.parrot.car.filemanager",
                "com.parrot.car.filemanager.FileManagerActivity"));
        intent.putExtra("file_uri", uriStrings);
        if (isMusicFolder) {
            intent.putExtra("app_type", "Music");
        } else if (isMovieFolder) {
            intent.putExtra("app_type", "MTT");
        }
        intent.putExtra("is_move", false);
        startActivity(intent);
        selectedFileHashMap.clear();
    }

    private void rename() {
        String[] uriStrings = Utils.getUriStringForHashmap(selectedFileHashMap);
        Intent intent = new Intent("com.parrot.filemanager.action.RENAME");
        intent.setPackage("com.parrot.car.filemanager");
        intent.putExtra("file_uri", uriStrings);
        if (isMusicFolder) {
            intent.putExtra("app_type", "Music");
        } else if (isMovieFolder) {
            intent.putExtra("app_type", "MTT");
        }
        AppController.getInstance().getApplicationContext().startService(intent);
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

    private String getUriString(String filePath, boolean isAudioFile) {
        File file2 = new File(filePath);
        if (!file2.exists()) {
            Log.e("mytest", "file is not existing");
            return null;
        }
        Cursor cursor = null;

        if (isAudioFile) {
            cursor = AppController.getInstance().getApplicationContext().getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID},
                    MediaStore.Audio.Media.DATA + "=? ",
                    new String[]{filePath},
                    null);
        } else {
            cursor = AppController.getInstance().getApplicationContext().getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Video.Media._ID},
                    MediaStore.Video.Media.DATA + "=? ",
                    new String[]{filePath},
                    null);
        }
        Log.d("mytest", "Cursor = " + cursor);
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            if (isAudioFile) {
                int id = cursor.getInt(0);
                cursor.close();
                return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        "" + id).toString();
            } else {
                int id = cursor.getInt(0);
                cursor.close();
                return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        "" + id).toString();
            }
        }
        return null;
    }

}
