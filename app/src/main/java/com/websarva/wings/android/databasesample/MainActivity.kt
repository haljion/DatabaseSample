package com.websarva.wings.android.databasesample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {
    companion object {
        // 通知チャンネルID文字列定数
        private const val CHANNEL_ID = "sample_notification"
    }

    // 選択されたカクテルの主キーIDを表すプロパティ
    private var _cocktailId = -1
    // 選択されたカクテルの主キーIDを表すプロパティ
    private var _cocktailName = ""
    // データベースヘルパーオブジェクト
    private val _helper = DatabaseHelper(this@MainActivity)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // カクテルリスト用ListView(lvCocktail)を取得
        val lvCocktail = findViewById<ListView>(R.id.lvCocktail)
        // lvCocktailにリスナを登録
        lvCocktail.onItemClickListener = ListItemClickListner()

        // 通知チャネル名をstring.xmlから取得
        val name = getString(R.string.notification_channel_name)
        // 通知チャネルの重要度を標準に設定
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        // 通知チャネルを生成
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        // NotificationManagerオブジェクトを取得
        val manager = getSystemService(NotificationManager::class.java)
        // 通知チャネルを設定
        manager.createNotificationChannel(channel)
    }

    // 保存ボタンがタップされた時の処理メソッド
    fun onSaveButtonClick(view: View) {
        // 感想欄を取得
        val etNote = findViewById<EditText>(R.id.etNote)
        // 入力された感想を取得
        val note = etNote.text.toString()

        // データベースヘルパーオブジェクトからデータベース接続オブジェクトを取得
        val db = _helper.writableDatabase

        // まず、リストで選択されたカクテルのメモデータを削除。その後インサートを行う
        // 削除用SQL文字列を用意
        val sqlDelete = "DELETE FROM cocktailmemos WHERE _id = ?"
        // SQL文字列を元にプリペアドステートメントを取得
        var stmt = db.compileStatement(sqlDelete)
        // 変数のバインド
        stmt.bindLong(1, _cocktailId.toLong())
        // 削除SQLの実行
        stmt.executeUpdateDelete()

        // インサート用SQL文字列を用意
        val sqlInsert = "INSERT INTO cocktailmemos (_id, name, note) VALUES (?, ?, ?)"
        // SQL文字列を元にプリペアドステートメントを取得
        stmt = db.compileStatement(sqlInsert)
        // 変数のバインド
        stmt.bindLong(1, _cocktailId.toLong())
        stmt.bindString(2, _cocktailName)
        stmt.bindString(3, note)
        // インサートSQLの実行
        stmt.executeInsert()

        // 感想欄の入力値を消去
        etNote.setText("")
        // カクテル名を表示するTextViewを取得
        val tvCocktailName = findViewById<TextView>(R.id.tvCocktailName)
        // カクテル名を「未選択」に変更
        tvCocktailName.text = getString(R.string.tv_name)
        // 保存ボタンを取得
        val btnSave = findViewById<Button>(R.id.btnSave)
        // 保存ボタンをタップできないように変更
        btnSave.isEnabled = false
    }

    // リストがタップされたときの処理が記述されたメンバクラス
    private inner class ListItemClickListner: AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            // タップされた行番号をプロパティの主キーIDに代入
            _cocktailId = position
            // タップされた行のデータを取得。これがカクテル名となるので、プロパティに代入
            _cocktailName = parent.getItemAtPosition(position) as String
            // カクテル名を表示するTextViewを取得
            val tvCocktailName = findViewById<TextView>(R.id.tvCocktailName)
            // カクテル名を表示するTextViewをに表示カクテル名を設定
            tvCocktailName.text = _cocktailName
            // 保存ボタンを取得
            val btnSave = findViewById<Button>(R.id.btnSave)
            // 保存ボタンをタップできるように変更
            btnSave.isEnabled = true

            // データベースヘルパーオブジェクトからデータベース接続オブジェクトを取得
            val db = _helper.writableDatabase
            // 主キーによる検索SQL文字列を用意
            val sql = "SELECT * FROM cocktailmemos WHERE _id = ${_cocktailId}"
            // SQLの実行
            val cursor = db.rawQuery(sql, null)
            // データベースから取得した値を格納する変数の用意。データがなかったときのための初期値も用意
            var note = ""

            // SQL実行の戻り値であるcursorオブジェクトをループさせてデータベース内のデータを取得
            while (cursor.moveToNext()) {
                // カラムのインデックス値を取得
                val idxNote = cursor.getColumnIndex("note")
                // カラムのインデックス値を元に実際のデータを取得
                note = cursor.getString(idxNote)
            }

            // 感想のEditTextの各画面部品を取得しデータベースの値を反映
            val etNote = findViewById<EditText>(R.id.etNote)
            etNote.setText(note)
        }
    }

    override fun onDestroy() {
        _helper.close()
        super.onDestroy()
    }

    // 通知ボタンがタップされた時の処理メソッド
    fun onNotificationButtonClick(view: View) {
        val builder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
        // 通知エリアに表示されるアイコンを設定
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
        // 通知ドロワーでの表示タイトルを設定
        builder.setContentTitle(getString(R.string.notification_title_finish))
        // 通知ドロワーでの表示メッセージを設定
        builder.setContentText(getString(R.string.notification_text_finish))
        // BuilderからNotificationオブジェクトを生成
        val notification = builder.build()
        // NotificationManagerCompatオブジェクトを取得
        val manager = NotificationManagerCompat.from(this@MainActivity)
        // 通知
        manager.notify(100, notification)
    }
}