package com.jsp.movie.lesskey;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.android.inputmethodcommon.InputMethodSettingsFragment;
import com.jsp.facebook.FBActivity;
import com.jsp.kakao.KTActivity;

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
        Log.d ("ㄱ", Integer.toHexString('ㄱ'));
        Log.d ("ㅏ", Integer.toHexString('ㅏ'));
    }
    @Override
    protected boolean isValidFragment(final String fragmentName) {
        return Settings.class.getName().equals(fragmentName);
    }
    public static class Settings extends InputMethodSettingsFragment implements Preference.OnPreferenceClickListener {

        Preference server;
        Preference facebook;
        Preference kakao;

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

            server.setOnPreferenceClickListener(this);
            facebook.setOnPreferenceClickListener(this);
            kakao.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {

            if (preference == server) {

                Log.d ("movie", "Download from server");
            }
            else if (preference == facebook) {

                Intent i = new Intent (getActivity(), FBActivity.class);
                startActivity(i);
            }
            else if (preference == kakao) {

                Intent i = new Intent (getActivity(), KTActivity.class);
                startActivity(i);
            }
            else
                return false;

            return true;
        }
    }
}