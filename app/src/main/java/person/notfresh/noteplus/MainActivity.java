package person.notfresh.noteplus;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import person.notfresh.noteplus.db.NoteDbHelper;

public class MainActivity extends AppCompatActivity {
    private EditText momentEditText;
    private Button saveButton;
    private ListView momentsListView;
    private NoteDbHelper dbHelper;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化视图
        momentEditText = findViewById(R.id.momentEditText);
        saveButton = findViewById(R.id.saveButton);
        momentsListView = findViewById(R.id.momentsListView);
        dbHelper = new NoteDbHelper(this);

        // 加载现有记录
        loadMoments();

        // 设置保存按钮点击监听器
        saveButton.setOnClickListener(v -> saveMoment());

        // 设置输入框的回车键监听
        momentEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveMoment();
                return true;
            }
            return false;
        });
    }

    /**
     * 保存当前输入的时刻记录
     */
    private void saveMoment() {
        String content = momentEditText.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前时间
        long timestamp = System.currentTimeMillis();

        // 保存到数据库
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NoteDbHelper.COLUMN_CONTENT, content);
        values.put(NoteDbHelper.COLUMN_TIMESTAMP, timestamp);

        long newRowId = db.insert(NoteDbHelper.TABLE_NOTES, null, values);

        if (newRowId != -1) {
            Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show();
            momentEditText.setText(""); // 清空输入框
            loadMoments(); // 重新加载列表
        } else {
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 加载所有时刻记录
     */
    private void loadMoments() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                NoteDbHelper.TABLE_NOTES,
                new String[]{NoteDbHelper.COLUMN_ID, NoteDbHelper.COLUMN_CONTENT, NoteDbHelper.COLUMN_TIMESTAMP},
                null, null, null, null,
                NoteDbHelper.COLUMN_TIMESTAMP + " DESC" // 按时间戳倒序排列
        );

        String[] fromColumns = {NoteDbHelper.COLUMN_TIMESTAMP, NoteDbHelper.COLUMN_CONTENT};
        int[] toViews = {R.id.timestampText, R.id.contentText};

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.note_list_item,
                cursor,
                fromColumns,
                toViews,
                0
        );

        // 设置时间戳格式
        adapter.setViewBinder((view, cursor1, columnIndex) -> {
            if (columnIndex == cursor1.getColumnIndex(NoteDbHelper.COLUMN_TIMESTAMP)) {
                long timestamp = cursor1.getLong(columnIndex);
                String formattedDate = formatTimestamp(timestamp);
                ((TextView) view).setText(formattedDate);
                return true;
            }
            return false;
        });

        momentsListView.setAdapter(adapter);
    }

    /**
     * 格式化时间戳为指定格式
     */
    private String formatTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
        String dateStr = dateFormat.format(new Date(timestamp));
        
        // 获取星期
        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (dayOfWeek < 0) dayOfWeek = 0;
        
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
        String timeStr = timeFormat.format(new Date(timestamp));
        
        return dateStr + "，星期" + weekDays[dayOfWeek] + "，" + timeStr;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
} 