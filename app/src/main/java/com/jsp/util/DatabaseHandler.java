package com.jsp.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.NoSuchElementException;

import android.os.Handler;
import android.os.Message;

public class DatabaseHandler {

    private SQLiteDatabase db;

    public DatabaseHandler(SQLiteDatabase db) {
        this.db = db;
    }

    public void CandidateListGenerator(List<String> rtn, String word, String previous) {

        String SQL = "select * "
                + "from Private "
                + "where initial = '" + word + "' "
                + "order by priority desc "
                + "limit 10";
        Cursor cursor = db.rawQuery(SQL, null);
        int resultCnt = cursor.getCount();
        Log.d("check", "count "+ resultCnt);
        if(resultCnt > 0)
        {
            for(int i=0;i<resultCnt;i++)
            {
                cursor.moveToNext();
                rtn.add(cursor.getString(1));
            }
        }
        int temp = resultCnt;

        cursor.close();

        if(resultCnt < 5)
        {
            SQL = "select * "
                    + "from Server "
                    + "where initial = '" + word + "' ";
            if(temp > 0) {
                SQL += "and word not in (";
                for(int i=0;i<temp;i++)
                {
                    SQL += "'" + rtn.get(i) + "'";
                    if(temp > i + 1)
                        SQL += ", ";
                    else
                        SQL += ") ";
                }
            }
            SQL += "order by access_num desc "
                    + "limit " + (5 - resultCnt);
            Log.d("check", SQL);
            cursor = db.rawQuery(SQL, null);
            resultCnt = cursor.getCount();
            if(resultCnt > 0)
            {
                for(int i=0;i<resultCnt;i++)
                {
                    cursor.moveToNext();
                    rtn.add(cursor.getString(1));
                    int j;
                    for(j=0;j<temp;j++)
                        if(cursor.getString(1).compareTo(rtn.get(j)) == 0)
                            break;
                    if(temp == j)
                    {
                        //rtn.add(cursor.getString(1));
                        SQL = "insert into Private "
                                + "values('" + word + "', '" + rtn.get(temp+i) + "', " + (500 - i * 100) + ", 0)";
                        Log.d("hoero", SQL);
                        db.execSQL(SQL);
                    }
                }
            }

            cursor.close();
        }
        if(previous != null) {
            int num=0;
            for(int i=0;i<rtn.size();i++) {
                SQL = "select word "
                        + "from Context "
                        + "where word = '" + rtn.get(i) + "' "
                        + "and (pre1 = '" + previous + "' "
                        + "or pre2 = '" + previous + "' "
                        + "or pre3 = '" + previous + "' "
                        + "or pre4 = '" + previous + "' "
                        + "or pre5 = '" + previous + "')";
                Log.d("pre", SQL);
                cursor = db.rawQuery(SQL, null);
                if(cursor.getCount() > 0) {
                    String str = new String(rtn.get(i));
                    for(int j=i;j>num;j--) {
                        rtn.set(j, rtn.get(j-1));
                    }
                    rtn.set(num, str);
                    num++;
                }
                cursor.close();
            }
        }
        for(int i=rtn.size()-1;i>4;i--) {
            Log.d("Size", ""+rtn.size());

            rtn.remove(i);
        }
    }

    public void onCandidateSelected(String s, String previous) {
        String SQL = "select * "
                + "from Private "
                + "where word = '" + s + "'";
        Log.d("select", s);
        Cursor cursor = db.rawQuery(SQL, null);
        Log.d("ab", "ab");
        cursor.moveToNext();
        Log.d("kk", "kk");
        SQL = "update Private "
                + "set priority = priority + ";
        if(cursor.getInt(3) >= 0){
            SQL += "50 , balloon = 0 ";
        }
        else{
            SQL += "100 , balloon = balloon - 50 ";
        }

        SQL += "where word = '" + s + "'";
        Log.d("dc", "dc");
        db.execSQL(SQL);
        if(cursor.getInt(2) >= 1000) {
            SQL = "update Private "
                    + "set priority = priority / 2, balloon = balloon - 50 "
                    + "where initial = '" + cursor.getString(0) + "'";
            db.execSQL(SQL);
            Log.d("balloon", "half");
        }
        cursor.close();
        Log.d("ac", "ac");
        if(previous != null)
        {
            SQL = "select * "
                    + "from Context "
                    + "where word = '" + s + "'";
            Log.d("SQL1", SQL);
            cursor = db.rawQuery(SQL, null);

            int count = cursor.getCount();

            if(count == 0) {
                SQL = "insert into Context(word, pre1, age1) "
                        + "values('" + s + "', '" + previous + "', 1)";
                Log.d("SQL2", SQL);
                db.execSQL(SQL);
            } else {
                cursor.moveToNext();
                int i;
                for(i=1;i<=5;i++)
                    if(!cursor.isNull(i) && cursor.getString(i).compareTo(previous)==0) {
                        SQL = "update Context "
                                + "set age" + i + " = 1";
                        for(int j=1;j<=5;j++)
                            if(!cursor.isNull(j+5) && cursor.getInt(i+5) > cursor.getInt(j+5))
                                SQL += ", age" + j + " = " + (cursor.getInt(j+5) + 1);
                        SQL += " where word = '" + s + "'";
                        Log.d("cursor", "Update exixting pre");
                        break;
                    } else if(cursor.isNull(i)) {
                        SQL = "update Context "
                                + "set pre" + i + " = '" + previous + "'";
                        for(int j=1;j<i;j++)
                            SQL += " , age" + j + " = " + (cursor.getInt(j+5) + 1);
                        SQL += ", age" + i + " = 1"
                                + " where word = '" + s + "'";
                        Log.d("cursor", "First input");
                        break;
                    }
                if(i == 6) {
                    int j, tmp;
                    for(j=6;j<=10;j++)
                        if(cursor.getInt(j) == 5)
                            break;
                    tmp = j-5;
                    SQL = "update Context "
                            + "set pre" + tmp + " = '" + previous + "'";
                    for(j=1;j<=5;j++) {
                        SQL += " , age" + j + " = ";
                        if(tmp == j)
                            SQL += 1;
                        else
                            SQL += (cursor.getInt(j+5) + 1);
                    }
                    SQL += " where word = '" + s + "'";
                }
                Log.d("SQL3", SQL);
                db.execSQL(SQL);
            }
            cursor.close();
        }
    }

    public void onNewWordGenerated(String newWord) {

        String SQL = "select * "
                + "from Private "
                + "where word = '" + newWord + "'";
        Cursor cursor = db.rawQuery(SQL, null);

        int count = cursor.getCount();

        cursor.close();

        if(count == 0) {
            SQL = "insert into Private "
                    + "values('";
            int len = newWord.length();
            boolean exist = true;

            for (int i = 0; i < len; i++) {

                char ch = getChosung (newWord.charAt(i));
                if (ch == '.') {
                    exist = false;
                    break;
                }

                SQL += ch;
            }
            SQL += "', '" + newWord + "', 200, 100)";

            if (!exist) return;

        } else {
            SQL = "update Private "
                    + "set priority = priority + 100 "
                    + "where word = '" + newWord + "'";
        }
        Log.d("debug", SQL);
        db.execSQL(SQL);
    }

    public void ServerTableInitializer(File server,Handler handler) {


        try {
            LineNumberReader  lnr = new LineNumberReader(new FileReader(server));
            lnr.skip(Long.MAX_VALUE);
            int max = lnr.getLineNumber() + 1;

            handler.sendMessage(Message.obtain(handler,max));//Add 1 because line index starts at 0
// Finally, the LineNumberReader object should be closed to prevent resource leak
            lnr.close();
            Scanner sc = new Scanner(server);

            while (sc.hasNext()) {
                String SQL = "insert or replace into Server "
                        + "values('" + sc.next() + "', '" + sc.next() + "', '" + sc.next() +"')";
                db.execSQL(SQL);
                handler.sendMessage(Message.obtain(handler,-1));
            }

            handler.sendMessage(Message.obtain(handler,-2));

            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (NoSuchElementException e){
            Log.e("db"+e.toString(),"hello");

            e.printStackTrace();
        }
    }



    public void PrivateDataExport(List<String> rtn,Handler handler) {

        String SQL = "select * "
                + "from Private "
                + "order by priority desc ";
        Cursor cursor = db.rawQuery(SQL, null);
        int resultCnt = cursor.getCount();
        handler.sendMessage(Message.obtain(handler,resultCnt));
        Log.d("check", "count " + resultCnt);
        if(resultCnt > 0)
        {
            for(int i=0;i<resultCnt;i++)
            {
                cursor.moveToNext();
                rtn.add(cursor.getString(0));
                rtn.add("\t");
                rtn.add(cursor.getString(1));
                rtn.add("\t");
                rtn.add(cursor.getString(2));
                rtn.add("\t");
                rtn.add(cursor.getString(3));
                rtn.add("\n");
                handler.sendMessage(Message.obtain(handler,-1));
            }
        }
        else{
            rtn.add("empty");
        }

        handler.sendMessage(Message.obtain(handler,-2));


        cursor.close();


    }


    public void PrivateTableInitializer(File user, Handler handler) {
        try {
            LineNumberReader  lnr = new LineNumberReader(new FileReader(user));
            lnr.skip(Long.MAX_VALUE);
            int max = lnr.getLineNumber() + 1;

            handler.sendMessage(Message.obtain(handler,max));//Add 1 because line index starts at 0
// Finally, the LineNumberReader object should be closed to prevent resource leak
            lnr.close();


            Scanner sc = new Scanner(user);
            while (sc.hasNext()) {
                String SQL = "insert or replace into Private "
                        + "values('" + sc.next() + "', '" + sc.next() + "', '" + sc.next() +  "', '" + sc.next() +"')";
                db.execSQL(SQL);
                handler.sendMessage(Message.obtain(handler, -1));
            }

            handler.sendMessage(Message.obtain(handler,-2));
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private final static char choTable[] = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    char getChosung (char c) {

        int idx = (c - 0xAC00)/28/21;

        if (idx < 0 || idx >= choTable.length)
            return '.';

        return choTable[idx];
    }

}