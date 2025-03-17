package person.notfresh.noteplus.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NoteDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String DATABASE_CREATE = "create table "
            + TABLE_NOTES + "(" 
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_CONTENT + " text not null, "
            + COLUMN_TIMESTAMP + " integer not null);";

    public NoteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }
} 