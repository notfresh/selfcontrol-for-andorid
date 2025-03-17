package person.notfresh.noteplus;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import person.notfresh.noteplus.db.NoteDbHelper;

public class NoteListActivity extends AppCompatActivity {
    private NoteDbHelper dbHelper;
    private ListView noteListView;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        dbHelper = new NoteDbHelper(this);
        noteListView = findViewById(R.id.noteListView);

        loadNotes();

        noteListView.setOnItemClickListener((parent, view, position, id) -> {
            // 由于新设计中已不需要此功能，此处留空
        });
    }

    private void loadNotes() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            NoteDbHelper.TABLE_NOTES,
            new String[]{NoteDbHelper.COLUMN_ID, NoteDbHelper.COLUMN_CONTENT, NoteDbHelper.COLUMN_TIMESTAMP},
            null, null, null, null,
            NoteDbHelper.COLUMN_TIMESTAMP + " DESC"
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

        adapter.setViewBinder((view, cursor1, columnIndex) -> {
            if (columnIndex == cursor1.getColumnIndex(NoteDbHelper.COLUMN_TIMESTAMP)) {
                long timestamp = cursor1.getLong(columnIndex);
                String formattedDate = formatTimestamp(timestamp);
                ((TextView) view).setText(formattedDate);
                return true;
            }
            return false;
        });

        noteListView.setAdapter(adapter);
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