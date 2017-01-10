package gallery.picker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import static android.provider.MediaStore.Images.Media.DATA;
import static android.provider.MediaStore.Images.Media.DATE_ADDED;
import static android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
import static android.provider.MediaStore.Images.Media.MIME_TYPE;
import static android.provider.MediaStore.Images.Media.SIZE;
import static android.provider.MediaStore.Images.Media._ID;


/**
 * Created at 2016/5/3.
 *
 * @author YinLanShan
 */
public class ImagePicker extends Activity implements
        View.OnClickListener, AdapterView.OnItemClickListener {
    public static final String EXTRA_MAX = "max";
    public static final String EXTRA_WITH_CAMERA = "camera";
    public static final String EXTRA_RESULT = "result";

    private static final int REQUEST_CAMERA = 1;

    private int mMax = -1;
    private boolean withCamera;

    private ArrayList<Folder> mFolders;
    private PopupWindow mFolderPopupWindow;
    private ImageGridAdapter mAdapter;
    private File mPhoto;

    private TextView mCategoryText;
    private Button mDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mMax = intent.getIntExtra(EXTRA_MAX, -1);
        withCamera = intent.getBooleanExtra(EXTRA_WITH_CAMERA, true);

        setContentView(R.layout.image_picker);
        findViewById(R.id.iv_back).setOnClickListener(this);
        mDone = (Button) findViewById(R.id.bt_commit);
        mDone.setOnClickListener(this);
        mCategoryText = (TextView) findViewById(R.id.tv_category);
        mCategoryText.setOnClickListener(this);
        mAdapter = new ImageGridAdapter(this);
        mAdapter.setShowCamera(withCamera);
        mAdapter.setSingleChoice(mMax == 1);
        GridView gv = (GridView)findViewById(R.id.gv_images);
        gv.setAdapter(mAdapter);
        gv.setOnItemClickListener(this);
        new Loader().execute();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.iv_back) {
            finish();
            setResult(RESULT_CANCELED);
        }
        else if(id == R.id.bt_commit){
            HashSet<Integer> selected = mAdapter.getSelected();
            TreeMap<Integer, String> map = new TreeMap<>();
            for(Integer index : selected)
                map.put(index, (String)mAdapter.getItem(index));
            ArrayList<String> result = new ArrayList<>(map.values());
            Intent intent = new Intent();
            intent.putStringArrayListExtra(EXTRA_RESULT, result);
            setResult(RESULT_OK, intent);
            finish();
        }
        else if(id == R.id.tv_category) {
            if(mFolderPopupWindow != null && mFolderPopupWindow.isShowing())
                mFolderPopupWindow.dismiss();
            else
                showFolderList();
        }
    }

    private void showFolderList() {
        if(mFolderPopupWindow == null) {
            mFolderPopupWindow = createPopupFolderList();
        }
        mFolderPopupWindow.showAsDropDown(mCategoryText);
    }

    private PopupWindow createPopupFolderList() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = (int) (dm.heightPixels*(4.5f/8.0f));
        PopupWindow popupWindow = new PopupWindow(this);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        FolderAdapter folderAdapter = new FolderAdapter(this);
        folderAdapter.setFolders(mFolders);
        ListView lv = new ListView(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv.setAdapter(folderAdapter);
        lv.setItemChecked(0, true);
        popupWindow.setContentView(lv);
        popupWindow.setWidth(width);
        popupWindow.setHeight(height);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Folder folder = (Folder)adapterView.getItemAtPosition(i);
                mCategoryText.setText(folder.name);
                mAdapter.setData(folder.images);
                updateFinishButton();

                adapterView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFolderPopupWindow.dismiss();
                    }
                }, 100);
            }
        });
        return popupWindow;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mAdapter.isShowCamera()) {
            if(position == 0) {
                startCamera();
                return;
            }
        }

        if(mMax != -1) {
            HashSet<Integer> selected = mAdapter.getSelected();
            if(selected.size() == mMax && !selected.contains(position)) {
                return;
            }
        }
        mAdapter.select(position);
        if(mMax == 1) {
            onClick(findViewById(R.id.bt_commit));
            return;
        }

        updateFinishButton();
    }

    private void updateFinishButton() {
        HashSet<Integer> selected = mAdapter.getSelected();
        int count = selected.size();
        if(count == 0) {
            mDone.setText("完成");
            mDone.setEnabled(false);
            return;
        }

        if(mMax == -1) {
            mDone.setText(String.format("完成(%s)", count));
        }
        else {
            mDone.setText(String.format("完成(%s/%s)", count, mMax));
        }
        mDone.setEnabled(true);
    }

    private void startCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mPhoto = getCameraOutputFile();
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mPhoto));
        startActivityForResult(cameraIntent, REQUEST_CAMERA);
    }

    private File getCameraOutputFile() {
        File dir;
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            dir = new File(dir, "Camera");
        }else{
            dir = getCacheDir();
        }

        String fileName = String.format("IMG_%1$tY%1$tm%1$td_%1$tH%1$tM%1$tS%1$tL.jpg",
                Calendar.getInstance());
        return new File(dir, fileName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK && requestCode == REQUEST_CAMERA) {
            Uri uri = Uri.fromFile(mPhoto);
            if(mPhoto != null && !mPhoto.exists())
                return;
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            ArrayList<String> result = new ArrayList<>(1);
            result.add(mPhoto.getPath());
            Intent intent = new Intent();
            intent.putStringArrayListExtra(EXTRA_RESULT, result);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    class Loader extends AsyncTask<Void, Void, ArrayList<Folder>> {
        @Override
        protected ArrayList<Folder> doInBackground(Void... params) {
            String[] projection = {_ID, DATA};
            String selection = String.format("%s>0 and %s=? or %s=?",
                    SIZE, MIME_TYPE, MIME_TYPE);
            String[] args = {"image/jpeg", "image/png"};
            Cursor cursor = getContentResolver().query(EXTERNAL_CONTENT_URI, projection,
                    selection, args, DATE_ADDED + " desc");
            ArrayList<Folder> folders = null;
            if(cursor != null) {
                if(cursor.getCount() > 0) {
                    Folder all = new Folder();
                    all.name = "所有图片";
                    all.path = "/";
                    HashMap<String, Folder> map = new HashMap<>();
                    while (cursor.moveToNext()) {
                        String path = cursor.getString(1);
                        all.images.add(path);
                        int index = path.lastIndexOf('/');
                        if(index > 0) {
                            String dir = path.substring(0, index);
                            Folder folder = map.get(dir);
                            if(folder == null) {
                                folder = new Folder();
                                folder.path = dir;
                                index = dir.lastIndexOf('/');
                                String name;
                                if(index == -1) {
                                    name = dir;
                                }
                                else {
                                    name = dir.substring(index + 1);
                                }
                                folder.name = name;
                                map.put(dir, folder);
                            }
                            folder.images.add(path);
                        }
                    }
                    folders = new ArrayList<>(map.size());
                    folders.addAll(map.values());
                    Collections.sort(folders, new Comparator<Folder>() {
                        @Override
                        public int compare(Folder lhs, Folder rhs) {
                            return lhs.path.compareTo(rhs.path);
                        }
                    });
                    folders.add(0, all);
                }
                cursor.close();
            }
            return folders;
        }

        @Override
        protected void onPostExecute(ArrayList<Folder> folders) {
            if(folders == null) {
                mFolders = new ArrayList<>();
                mAdapter.setData(new ArrayList<String>());
            }
            mFolders = folders;
            mAdapter.setData(folders.get(0).images);
        }
    }

    static class Folder {
        String name;
        String path;
        ArrayList<String> images = new ArrayList<>();
    }
}
