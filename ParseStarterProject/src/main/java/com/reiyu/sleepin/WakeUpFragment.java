package com.reiyu.sleepin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Satomi on 1/3/16.
 */
public class WakeUpFragment extends AppCompatActivity {
    String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setAlarms();

        setContentView(R.layout.fragment_wake_up);
        Button button = (Button) findViewById(R.id.save_record);

        DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();
        date = year + "/" + (month + 1) + "/" + day;

        TimePicker tp1 = (TimePicker) findViewById(R.id.go_to_bed);
        tp1.setCurrentHour(23);
        tp1.setCurrentMinute(00);
        tp1.setIs24HourView(true);

        TimePicker tp2 = (TimePicker) findViewById(R.id.wake_up);
        tp2.setIs24HourView(true);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAveScore();
                groupSync();

                TimePicker tp1 = (TimePicker) findViewById(R.id.go_to_bed);
                int hour = tp1.getCurrentHour();
                int minute = tp1.getCurrentMinute();
                String go_to_bed_time = hour + ":" + minute;

                TimePicker tp2 = (TimePicker) findViewById(R.id.wake_up);
                hour = tp2.getCurrentHour();
                minute = tp2.getCurrentMinute();
                String wake_up_time = hour + ":" + minute;

                EditText memoText = (EditText) findViewById(R.id.memo);
                String memo = memoText.getText().toString();

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WakeUpFragment.this);
                String username = sp.getString("@string/username", null);

                if (username != null) {
                    ParseObject testObject = new ParseObject("SleepRecord");
                    testObject.put("date", date);
                    testObject.put("go_to_bed", go_to_bed_time);
                    testObject.put("wake_up", wake_up_time);
                    testObject.put("memo", memo);
                    testObject.put("username", username);
                    testObject.saveInBackground(new SaveCallback() {
                        public void done(ParseException e) {
                            if (e == null) {
                                Log.e("Sleep Record", "Successfully saved");
                                wakeUp(date);
                            } else {
                                // Sign up didn't succeed. Look at the ParseException
                                // to figure out what went wrong
                                Log.e("Sleep Record", "Error", e);
                                startActivity(new Intent(WakeUpFragment.this, WakeUpFragment.class));
                            }
                        }
                    });
                } else {
                    Log.e("Sleep Record", "username is null");
                    Toast.makeText(WakeUpFragment.this, "User info was empty. Please Sign in again.", Toast.LENGTH_SHORT);
                    startActivity(new Intent(WakeUpFragment.this, SignInFragment.class));
                }
            }
        });
    }

    private void wakeUp(String date) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putString("@string/record_updated", date).commit();
        sp.edit().putInt("@string/healthy_score", 100).commit();

        Toast.makeText(WakeUpFragment.this, "Sleep Record is successfully saved.", Toast.LENGTH_SHORT);

        startActivity(new Intent(WakeUpFragment.this, MainActivity.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sign_out) {
            signOut();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        System.exit(0);
    }

    private void signOut() {
        ParseUser.logOut();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putBoolean("@string/signed_in", false).commit();
        sp.edit().putString("@string/username", null).commit();
        sp.edit().putInt("@string/group_id", -1).commit();
        sp.edit().putString("@string/email", null).commit();
        startActivity(new Intent(WakeUpFragment.this, SignInFragment.class));
    }

    private void getAveScore() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SleepinessRecord");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WakeUpFragment.this);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String yesterday = year + "/" + (month + 1) + "/" + day;

        query.whereEqualTo("username", sp.getString("@string/username", null));
        query.whereEqualTo("date", yesterday);

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> scoreList, ParseException e) {
                if (e == null) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WakeUpFragment.this);
                    int ave;

                    if (scoreList.size() > 0) {
                        int sum = 0;
                        for (ParseObject score : scoreList) {
                            sum += score.getInt("score");
                        }
                        ave = sum / scoreList.size();

                        Log.e("Average Score", String.valueOf(ave));
                    } else {
                        Log.e("Average Score", "data was empty");
                        ave = 100;
                    }
                    sp.edit().putInt("@string/ave_score", ave);
                    storeAveScore(ave);
                } else {
                    Log.e("Average Score", "Error: " + e.getMessage());
                }
            }
        });
    }

    private void storeAveScore(int ave) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WakeUpFragment.this);
        int count = sp.getInt("@string/count", -1);
        int isPositive;

        if (ave > 60) {
            count += 1;
            isPositive = 1;
        } else if (ave <= 30) {
            count -= 1;
            isPositive = -1;
        } else {
            isPositive = 0;
        }
        showStamps(isPositive, count, ave);
        updateFlower(count);
        sp.edit().putInt("@string/count", count).commit();
    }

    private void updateFlower(int count) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WakeUpFragment.this);

        boolean hasClover2 = sp.getBoolean("@string/clover2", false);
        boolean hasButterfly2 = sp.getBoolean("@string/butterfly2", false);
        boolean hasClover = sp.getBoolean("@string/clover", false);
        boolean hasLadybug = sp.getBoolean("@string/ladybug", false);
        boolean hasButterfly = sp.getBoolean("@string/butterfly", false);
        boolean hasLeaf = sp.getBoolean("@string/leaf", false);
        boolean hasPot = sp.getBoolean("@string/pot", false);

        int untilNext;

        if (hasClover2) {
            if (count < 14) {
                sp.edit().putBoolean("@string/clover2", false);
                untilNext = 14 - count;
            } else {
                count = 14;
                untilNext = 0;
            }
        } else if (hasButterfly2) {
            if (count < 11) {
                sp.edit().putBoolean("@string/butterfly2", false);
                untilNext = 11 - count;
            } else if (count == 15) {
                sp.edit().putBoolean("@string/clover2", true);
                untilNext = -1;
            } else {
                untilNext = 15 - count;
            }
        } else if (hasClover) {
            if (count < 8) {
                sp.edit().putBoolean("@string/clover", false);
                untilNext = 8 - count;
            } else if (count == 11) {
                sp.edit().putBoolean("@string/butterfly2", true);
                untilNext = 15 - count;
            } else {
                untilNext = 11 - count;
            }
        } else if (hasLadybug) {
            if (count < 5) {
                sp.edit().putBoolean("@string/ladybug", false);
                untilNext = 5 - count;
            } else if (count == 8) {
                sp.edit().putBoolean("@string/clover", true);
                untilNext = 11 - count;
            } else {
                untilNext = 8 - count;
            }
        } else if (hasButterfly) {
            if (count < 3) {
                sp.edit().putBoolean("@string/butterfly", false);
                untilNext = 3 - count;
            } else if (count == 5) {
                sp.edit().putBoolean("@string/ladybug", true);
                untilNext = 8 - count;
            } else {
                untilNext = 5 - count;
            }
        } else if (hasLeaf) {
            if (count < 2) {
                sp.edit().putBoolean("@string/leaf", false);
                untilNext = 2 - count;
            } else if (count == 3) {
                sp.edit().putBoolean("@string/butterfly", true);
                untilNext = 5 - count;
            } else {
                untilNext = 3 - count;
            }
        } else if (hasPot) {
            if (count < 1) {
                sp.edit().putBoolean("@string/pot", false);
                untilNext = 2 - count;
            } else if (count == 2) {
                sp.edit().putBoolean("@string/leaf", true);
                untilNext = 3 - count;
            } else {
                untilNext = 2 - count;
            }
        } else {
            if (count == 1) {
                sp.edit().putBoolean("@string/pot", true);
                untilNext = 2 - count;
            } else {
                untilNext = 1 - count;
            }
        }

        sp.edit().putInt("@string/until_next", untilNext).commit();
        String username = sp.getString("@string/username", null);

        Log.e("storeAve untilNext", String.valueOf(untilNext));
        Log.e("storeAve count", String.valueOf(count));

        if (username != null) {
            ParseObject testObject = new ParseObject("FlowerRecord");
            testObject.put("username", username);
            testObject.put("date", date);
            testObject.put("clover2", hasClover2);
            testObject.put("butterfly2", hasButterfly2);
            testObject.put("clover", hasClover);
            testObject.put("ladybug", hasLadybug);
            testObject.put("butterfly", hasButterfly);
            testObject.put("leaf", hasLeaf);
            testObject.put("pot", hasPot);

            testObject.saveInBackground(new SaveCallback() {
                public void done(ParseException e) {
                    if (e == null) {
                        Log.e("Flower Record", "Successfully saved");
                    } else {
                        Log.e("Flower Record", "Error", e);
                    }
                }
            });
        } else {
            Log.e("Sleep Record", "username is null");
            startActivity(new Intent(WakeUpFragment.this, SignInFragment.class));
        }
    }

    private void setAlarms() {
        SessionReceiver.scheduleAlarms(this, 10, 31, 1);
        SessionReceiver.scheduleAlarms(this, 12, 01, 2);
        SessionReceiver.scheduleAlarms(this, 13, 31, 3);
        SessionReceiver.scheduleAlarms(this, 15, 01, 4);
        SessionReceiver.scheduleAlarms(this, 16, 31, 5);
        SessionReceiver.scheduleAlarms(this, 18, 01, 6);
        SessionReceiver.scheduleAlarms(this, 19, 31, 7);
        SessionReceiver.scheduleAlarms(this, 21, 01, 8);
        SessionReceiver.scheduleAlarms(this, 9, 00, 10);
    }

    private void groupSync() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WakeUpFragment.this);

        if (sp.getStringSet("@string/member_set", null) != null) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("FlowerRecord");
            query.whereContainedIn("username", sp.getStringSet("@string/member_set", null));
            query.orderByAscending("createdAt");

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> flowerRecordList, ParseException e) {
                    if (e == null) {
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WakeUpFragment.this);

                        String name;
                        boolean hasClover2;
                        boolean hasButterfly2;
                        boolean hasClover;
                        boolean hasLadybug;
                        boolean hasButterfly;
                        boolean hasLeaf;
                        boolean hasPot;

                        for (ParseObject flowerRecord : flowerRecordList) {
                            hasClover2 = flowerRecord.getBoolean("hasClover2");
                            hasButterfly2 = flowerRecord.getBoolean("hasButterfly2");
                            hasClover = flowerRecord.getBoolean("hasClover");
                            hasLadybug = flowerRecord.getBoolean("hasLadybug");
                            hasButterfly = flowerRecord.getBoolean("hasButterfly");
                            hasLeaf = flowerRecord.getBoolean("hasLeaf");
                            hasPot = flowerRecord.getBoolean("hasPot");

                            name = flowerRecord.getString("username");

                            HashSet<String> flowerState = new HashSet<>();
                            flowerState.add("1," + String.valueOf(hasClover2));
                            flowerState.add("2," + String.valueOf(hasButterfly2));
                            flowerState.add("3," + String.valueOf(hasClover));
                            flowerState.add("4," + String.valueOf(hasLadybug));
                            flowerState.add("5," + String.valueOf(hasButterfly));
                            flowerState.add("6," + String.valueOf(hasLeaf));
                            flowerState.add("7," + String.valueOf(hasPot));

                            sp.edit().putStringSet("@string/flower_state" + name, flowerState).commit();

                            if (flowerState.size() == 7) {
                                Log.e("GroupSync " + name, "successfully get" + flowerState.toString());
                            } else {
                                Log.e("GroupSync " + name, "failed" + flowerState.toString());
                            }
                        }
                    } else {
                        Log.e("GroupSync", "Error: " + e.getMessage());
                    }
                }
            });
        }
    }

    private void showStamps(int isPositive, int count, int ave) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("昨日の結果");
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        TextView textTitle = new TextView(this);
        textTitle.setText("昨日の平均点は" + ave + "でした！");

        TextView text1 = new TextView(this);
        if (isPositive > 0) {
            text1.setText("スタンプをゲットしました！おめでとう :)");
        } else if (isPositive < 0) {
            text1.setText("スタンプが減ってしまいました！今日は頑張ろう！");
        } else {
            text1.setText("今日はスタンプを貰えるように頑張ろう！");
        }

        ImageView stamp = new ImageView(this);
        Bitmap bmp1 = null;
        switch (count) {
            case 1:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp1);
                break;
            case 2:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp2);
                break;
            case 3:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp3);
                break;
            case 4:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp4);
                break;
            case 5:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp5);
                break;
            case 6:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp6);
                break;
            case 7:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp7);
                break;
            case 8:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp8);
                break;
            case 9:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp9);
                break;
            case 10:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp10);
                break;
            case 11:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp11);
                break;
            case 12:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp12);
                break;
            case 13:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp13);
                break;
            default:
                bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.stamp);
                text1.setText("毎日頑張ってスタンプをためていこう！");
                break;
        }
        if (bmp1 != null) {
            stamp.setImageBitmap(bmp1);
            ll.addView(stamp);
        }

        ll.addView(textTitle);
        ll.addView(text1);

        alert.setView(ll);
        alert.setPositiveButton("OK", null);

        alert.show();
    }
}
