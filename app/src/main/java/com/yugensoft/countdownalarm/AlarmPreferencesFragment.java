package com.yugensoft.countdownalarm;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.List;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AlarmPreferencesFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AlarmPreferencesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlarmPreferencesFragment extends PreferenceFragment {
    private static final String TAG = "alarm-pref-fragment";

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
    private int mHour;
    private int mMinute;
    private Set<String> mRepeatDays;

    private OnFragmentInteractionListener mListener;

    private TimePicker mTimePicker;
    private Preference mPrefMessage;

    public AlarmPreferencesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param alarmId Parameter 1.
     * @return A new instance of fragment AlarmPreferencesFragment.
     */
    public static AlarmPreferencesFragment newInstance(long alarmId) {
        AlarmPreferencesFragment fragment = new AlarmPreferencesFragment();
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
        }

        // Get alarm from database
        daoSession = ((CountdownAlarmApplication)getActivity().getApplication()).getDaoSession();
        AlarmDao alarmDao = daoSession.getAlarmDao();
        List<Alarm> alarmList = alarmDao.queryBuilder().where(AlarmDao.Properties.Id.eq(mAlarmId)).list();
        mAlarm = alarmList.get(0);

        // construct the preferencefragment (internal)
        addPreferencesFromResource(R.xml.alarm_preferences);

        /**
         * Manual connection of the preferences to the database
         * Preference storage system isn't used as all preferences are set to FALSE persistent
         */

        mPrefMessage = getPreferenceManager().findPreference("message");
        final Long messageId = mAlarm.getMessageId();
        if(messageId != null){
            String taggedText = mAlarm.getMessage().getText();
            mPrefMessage.setSummary(MessageActivity.renderTaggedText(taggedText,daoSession.getTagDao(),getActivity()).toString());
        }
        mPrefMessage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(MessageActivity.newIntent(getActivity(),messageId), REQ_MESSAGE);
                return true;
            }
        });


        final MultiSelectListPreference prefRepeat = (MultiSelectListPreference)getPreferenceManager().findPreference("repeat");
        mRepeatDays = mAlarm.getScheduleRepeatDays().fullWords;
        prefRepeat.setValues(mRepeatDays);
        prefRepeat.setSummary(mAlarm.getScheduleRepeatDays().humanReadable);
        prefRepeat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mRepeatDays = (Set<String>)newValue;
                mAlarm.setSchedule(mHour,mMinute,mRepeatDays);
                prefRepeat.setSummary(mAlarm.getScheduleRepeatDays().humanReadable);
                return true;
            }
        });
        // ringtone
        final ExtRingtonePreference prefRingtone = (ExtRingtonePreference)getPreferenceManager().findPreference("ringtone");
        prefRingtone.setInitialRingtone(mAlarm.getRingtone());
        prefRingtone.setSummary(getRingtoneTitleFromUri(mAlarm.getRingtone()));
        prefRingtone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mAlarm.setRingtone((String)newValue);
                prefRingtone.setSummary(getRingtoneTitleFromUri(mAlarm.getRingtone()));
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

    private String getRingtoneTitleFromUri(String uri) {
        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), Uri.parse(uri));
        return ringtone.getTitle(getActivity());
    }

    // TODO: Rename method, update argument and hook method into UI event
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView lv = (ListView) view.findViewById(android.R.id.list);
        View header = getActivity().getLayoutInflater().inflate(R.layout.header_alarm_time_picker, null);
        lv.addHeaderView(header);

    }

    public void saveAlarm(){
        // assume on save that the alarm is to be activated too
        mAlarm.setActive(true);
        // save it
        daoSession.insertOrReplace(mAlarm);

        AlarmTimeFormatter.getNextAlarmTime(mAlarm.getNextAlarmTime(),true,getActivity());
    }


}
