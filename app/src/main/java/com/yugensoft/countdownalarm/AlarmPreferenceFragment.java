package com.yugensoft.countdownalarm;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TimePicker;

import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AlarmPreferenceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AlarmPreferenceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlarmPreferenceFragment extends PreferenceFragment {
    private static final String TAG = "app-debug";

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_ALARM_ID = "alarm-id";

    // request codes
    public static final int REQ_MESSAGE = 0;

    private DaoSession daoSession;
    // Alarm ID passed into this fragment
    private long mAlarmId;
    // All alarm parameters stored here, with exception of time/repeats, which are combined into schedule
    private Alarm mAlarm;

    // Time/repeats temporary store-on-change
    // Reloaded in onCreate(), so doesn't need to be in savedInstanceState
    int mHour;
    int mMinute;
    Set<String> mRepeatDays;

    private OnFragmentInteractionListener mListener;

    private TimePicker mTimePicker;
    private Preference mPrefMessage;

    public AlarmPreferenceFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param alarmId Parameter 1.
     * @return A new instance of fragment AlarmPreferenceFragment.
     */
    public static AlarmPreferenceFragment newInstance(long alarmId) {
        AlarmPreferenceFragment fragment = new AlarmPreferenceFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ALARM_ID, alarmId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mAlarmId = getArguments().getLong(ARG_ALARM_ID);
        } else {
            throw new RuntimeException("Expected bundle arguments.");
        }

        // Get alarm from database
        daoSession = ((CountdownAlarmApplication)getActivity().getApplication()).getDaoSession();
        AlarmDao alarmDao = daoSession.getAlarmDao();
        if(mAlarmId == -1L){
            // Add a new alarm
            mAlarm = Alarm.newDefaultAlarm();
        } else {
            // Get existing alarm
            mAlarm = alarmDao.loadByRowId(mAlarmId);
        }

        // construct the preferencefragment (internal)
        addPreferencesFromResource(R.xml.preferences_alarm);

        /**
         * Manual connection of the preferences to the database
         * Preference storage system isn't used as all preferences are set to FALSE persistent
         */

        mPrefMessage = getPreferenceManager().findPreference("message");
        if(mAlarm.getMessageId() != null){
            String taggedText = mAlarm.getMessage().getText();
            mPrefMessage.setSummary(MessageActivity.renderTaggedText(taggedText,daoSession.getTagDao(),getActivity(),null).toString());
        }
        mPrefMessage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(MessageActivity.newIntent(getActivity(),mAlarm.getMessageId()), REQ_MESSAGE);
                return true;
            }
        });


        final ExtMultiSelectListPreference prefRepeat = (ExtMultiSelectListPreference)getPreferenceManager().findPreference("repeat");
        mRepeatDays = mAlarm.getScheduleRepeatDays(getResources()).fullWords;
        prefRepeat.setValues(mRepeatDays);
        prefRepeat.setSummary(mAlarm.getScheduleRepeatDays(getResources()).humanReadable);
        prefRepeat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mRepeatDays = (Set<String>)newValue;
                mAlarm.setSchedule(mHour,mMinute,mRepeatDays);
                prefRepeat.setSummary(mAlarm.getScheduleRepeatDays(getResources()).humanReadable);
                return true;
            }
        });
        // ringtone
        final ExtRingtonePreference prefRingtone = (ExtRingtonePreference)getPreferenceManager().findPreference("ringtone");
        prefRingtone.setInitialRingtone(mAlarm.getRingtone());
        setRingtonePreferenceSummary(prefRingtone, mAlarm.getRingtone());
        prefRingtone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mAlarm.setRingtone((String)newValue);
                setRingtonePreferenceSummary(prefRingtone, mAlarm.getRingtone());
                return true;
            }
        });
        // vibrate
        CheckBoxPreference prefVibrate = (CheckBoxPreference)getPreferenceManager().findPreference("vibrate");
        prefVibrate.setChecked(mAlarm.getVibrate());
        prefVibrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mAlarm.setVibrate((boolean)newValue);
                return true;
            }
        });
        // label
        final EditTextPreference prefLabel = (EditTextPreference)getPreferenceManager().findPreference("label");
        prefLabel.setText(mAlarm.getLabel());
        prefLabel.setSummary(mAlarm.getLabel());
        prefLabel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mAlarm.setLabel((String)newValue);
                prefLabel.setText((String)newValue);
                prefLabel.setSummary((String)newValue);
                return true;
            }
        });

    }

    /**
     * Fills the summary of the ringtone preference with the title of the ringtone, if possible.
     * If not possible, just say "unknown".
     * @param prefRingtone The ExtRingtonePreference.
     * @param ringTone The ringtone URI string.
     */
    private void setRingtonePreferenceSummary(ExtRingtonePreference prefRingtone, String ringTone) {
        // handle "silent" ringtone
        if(ringTone.equals("")){
            prefRingtone.setSummary(R.string.silent);
            return;
        }

        String ringtoneTitle = getRingtoneTitleFromUri(ringTone);
        if(ringtoneTitle == null) {
            prefRingtone.setSummary(R.string.bracket_unknown);
        } else {
            prefRingtone.setSummary(ringtoneTitle);
        }

    }


    /**
     * Get the title of the ringtone from the ringtone uri
     * @param uri Ringtone uri.
     * @return The title; null if can't access that uri.
     */
    @Nullable
    private String getRingtoneTitleFromUri(String uri) {
        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), Uri.parse(uri));
        if(ringtone != null){
            return ringtone.getTitle(getActivity());
        } else {
            return null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // get the TimePicker from the parent activity
        mTimePicker = (TimePicker)getActivity().findViewById(R.id.alarm_time_picker);
        AlarmTimeType alarmTime = mAlarm.getScheduleAlarmTime(getActivity());
        mHour = alarmTime.hour;
        mTimePicker.setCurrentHour(mHour);
        mMinute = alarmTime.minute;
        mTimePicker.setCurrentMinute(mMinute);
        mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                mMinute=minute;
                mHour=hourOfDay;
                mAlarm.setSchedule(mHour,mMinute,mRepeatDays);
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQ_MESSAGE){
            switch (resultCode){
                case MessageActivity.RES_CANCELED:
                    // do nothing
                    break;
                case MessageActivity.RES_SAVED:
                    String messageText = data.getStringExtra(MessageActivity.RES_MESSAGE_TEXT);
                    mPrefMessage.setSummary(messageText);

                    long rawMessageId = data.getLongExtra(MessageActivity.RES_MESSAGE_ID, -1);
                    Long messageId = (rawMessageId == -1 ? null : rawMessageId);
                    Message message = null;
                    if(messageId != null){
                        message = daoSession.getMessageDao().loadByRowId(messageId);
                    }
                    mAlarm.setMessage(message);
                    break;
                default:
                    throw new RuntimeException("Unknown result");
            }
        }
    }


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView lv = (ListView) view.findViewById(android.R.id.list);
        View header = getActivity().getLayoutInflater().inflate(R.layout.header_alarm_time_picker, null);
        lv.addHeaderView(header);

    }

    /**
     * Method to save Alarm to db
     * @return Alarm ID.
     */
    public long saveAlarm(){
        // cement any time edits
        mTimePicker.clearFocus();

        // assume on save that the alarm is to be activated too
        mAlarm.setActive(true);

        // insert in case of new alarm
        if(mAlarmId == -1L){
            mAlarmId = daoSession.getAlarmDao().insert(mAlarm);
        } else {
            // save it if exists
            mAlarm.update();
        }

        TimeFormatters.getAlarmSetTime(mAlarm.getNextAlarmTime(),true,getActivity(),true);

        return mAlarmId;
    }


}
