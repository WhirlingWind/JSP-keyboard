package com.jsp.movie.lesskey;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.inputmethodcommon.InputMethodSettingsFragment;
import com.jsp.facebook.FBActivity;
import com.jsp.file.FDActivity;
import com.jsp.kakao.KTActivity;
import com.jsp.util.DatabaseHelper;

public class Setting extends PreferenceActivity {

    @Override
    public Intent getIntent() {
    final Intent modIntent = new Intent(super.getIntent());
    modIntent.putExtra(EXTRA_SHOW_FRAGMENT, Settings.class.getName());
    modIntent.putExtra(EXTRA_NO_HEADERS, true);
    return modIntent;
}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We overwrite the title of the activity, as the default one is "Voice Search".
        setTitle(R.string.setting_name);
    }

    @Override
    protected boolean isValidFragment(final String fragmentName) {
        return Settings.class.getName().equals(fragmentName);
    }
    public static class Settings extends InputMethodSettingsFragment implements Preference.OnPreferenceClickListener {

        Preference server;
        Preference facebook;
        Preference kakao;
        Preference delete;

        private DatabaseHelper dbHelper;
        private SQLiteDatabase db;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //setInputMethodSettingsCategoryTitle(R.string.language_selection_title);
            //setSubtypeEnablerTitle(R.string.select_language);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference);

            server = findPreference("d_server");
            facebook = findPreference("d_facebook");
            kakao = findPreference("d_kakao");
            delete = findPreference("d_del");

            server.setOnPreferenceClickListener(this);
            facebook.setOnPreferenceClickListener(this);
            kakao.setOnPreferenceClickListener(this);
            delete.setOnPreferenceClickListener(this);

            dbHelper = new DatabaseHelper(getActivity());
            try {
                dbHelper.createDataBase();
            } catch (Exception e) {

                e.printStackTrace();
            }
            db = dbHelper.openDataBase();
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {

            if (preference == server) {

                Intent i = new Intent (getActivity(), FDActivity.class);
                startActivity(i);
            }
            else if (preference == facebook) {

                Intent i = new Intent (getActivity(), FBActivity.class);
                startActivity(i);
            }
            else if (preference == kakao) {

                Intent i = new Intent (getActivity(), KTActivity.class);
                startActivity(i);
            }
            else if (preference == delete) {

                db.execSQL("delete from Private");
                db.execSQL("delete from Context");

                Toast.makeText(getActivity(), "All Data are Removed.", Toast.LENGTH_LONG).show();
            }
            else
                return false;

            return true;
        }
    }
}