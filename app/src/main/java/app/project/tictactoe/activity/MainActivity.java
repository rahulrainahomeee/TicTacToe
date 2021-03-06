package app.project.tictactoe.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import app.project.tictactoe.R;
import app.project.tictactoe.Utils.Constants;
import app.project.tictactoe.Utils.GameUtil;
import app.project.tictactoe.model.GoogleDB;

/**
 * Class for handing game with QR Code.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, ValueEventListener {

    private ImageView img[][] = new ImageView[3][3];
    private TextView txtPlayer1, txtPlayer2, txtScore;
    private LinearLayout layout1, layout2;
    private SharedPreferences s;
    private String mob;
    private int player = 0;
    private int red, green;
    private int M[][];
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private GoogleDB gdb = new GoogleDB();
    private int flag = 0;
    private Animation animMark;
    private MediaPlayer mp;
    private MediaPlayer pwin;
    private Vibrator vib;
    private ImageView imgWin, imgLose;
    private Toast toastWin, toastLose;
    private int myScore = 0, totalMatch = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA}, 1);

        img[0][0] = (ImageView) findViewById(R.id.img00);
        img[0][1] = (ImageView) findViewById(R.id.img01);
        img[0][2] = (ImageView) findViewById(R.id.img02);
        img[1][0] = (ImageView) findViewById(R.id.img10);
        img[1][1] = (ImageView) findViewById(R.id.img11);
        img[1][2] = (ImageView) findViewById(R.id.img12);
        img[2][0] = (ImageView) findViewById(R.id.img20);
        img[2][1] = (ImageView) findViewById(R.id.img21);
        img[2][2] = (ImageView) findViewById(R.id.img22);

        txtPlayer1 = (TextView) findViewById(R.id.txt_player_1);
        txtPlayer2 = (TextView) findViewById(R.id.txt_player_2);
        txtScore = (TextView) findViewById(R.id.score);

        layout1 = (LinearLayout) findViewById(R.id.activity_main);
        layout2 = (LinearLayout) findViewById(R.id.activity_main1);

        img[0][0].setOnClickListener(this);
        img[0][1].setOnClickListener(this);
        img[0][2].setOnClickListener(this);
        img[1][0].setOnClickListener(this);
        img[1][1].setOnClickListener(this);
        img[1][2].setOnClickListener(this);
        img[2][0].setOnClickListener(this);
        img[2][1].setOnClickListener(this);
        img[2][2].setOnClickListener(this);

        red = Color.RED;
        green = Color.GREEN;
        txtPlayer1.startAnimation(AnimationUtils.loadAnimation(this, R.anim.appear_player));
        txtPlayer2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.appear_player));
        animMark = AnimationUtils.loadAnimation(this, R.anim.mark_anim);

        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mp = MediaPlayer.create(this, R.raw.ting);
        pwin = MediaPlayer.create(this, R.raw.newgame);

        imgWin = new ImageView(this);
        imgLose = new ImageView(this);
        imgWin.setImageResource(R.drawable.win);
        imgLose.setImageResource(R.drawable.lost);

        toastWin = new Toast(this);
        toastLose = new Toast(this);

        toastWin.setView(imgWin);
        //toastWin.setText("Congrats. You Won...!");
        toastLose.setView(imgLose);
        //toastLose.setText("Try better in next match.");

        toastWin.setDuration(Toast.LENGTH_SHORT);
        toastLose.setDuration(Toast.LENGTH_SHORT);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Constants.mainActivity = null;

        s = getSharedPreferences("cache", 0);
        mob = s.getString("mob", "0000000000").trim();
        if (mob.contains("0000000000")) {
            //    startActivity(new Intent(this, LoginActivity.class));
        } else if (flag == 2) {     // Control from QRScan class.
            player = 2;
            flag = 0;
            resetGame();
            txtPlayer1.setText("Player 1");
            txtPlayer2.setText("Player 2 (You)");

            Toast.makeText(this, "Connected as Player 2", Toast.LENGTH_SHORT).show();
        } else if (flag == 1) {     // Control from QRGen Class.
            player = 1;
            flag = 0;
            initFirebase(mob);
            txtPlayer1.setText("Player 1 (You)");
            txtPlayer2.setText("Player 2");
            Toast.makeText(this, "Connected as Player 1", Toast.LENGTH_SHORT).show();
        } else {
            initFirebase(mob);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (flag == 0) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.menu_action_scan:
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA}, 1);
                Constants.mainActivity = this;  //required to write in GRTDB.
                flag = 2;   // Scan and connect as player 2.
                startActivity(new Intent(this, QRScan.class));
                break;

            case R.id.menu_action_gen:
                flag = 1; // gen and act as player 1
                if (mob.contains("0000000000")) {
                    Toast.makeText(this, "Verify your mobile number first.", Toast.LENGTH_SHORT).show();
                    break;
                }
                Constants.mob = mob;            // Only for generating QR.
                startActivity(new Intent(this, QRGen.class));
                break;

            default:
                break;
        }
        return true;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Log.d("44444444444444", "******************************************************");
        gdb = dataSnapshot.getValue(GoogleDB.class);
        if (gdb.getGameStatus() == 1) {     // CASE: Request for new game.
            gdb.setGameStatus(0); //When My QR (player1) is scanned.
            if (Constants.qrGen != null) {      //Close the QR Gen activity.
                Constants.qrGen.closeActivityCallback();
                return;
            }
            reflectToRTDB(0);

        } else if (gdb.getWon() > -1) { //CASE: GAME WON.
            layout1.startAnimation(AnimationUtils.loadAnimation(this, R.anim.next_game1));
            layout2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.next_game2));
            switch (gdb.getWon()) {
                case 0:
                    Toast.makeText(this, "Game Draw...!", Toast.LENGTH_SHORT).show();
                    vib.vibrate(100);
                    break;
                case 1:
                    if (gdb.getPlayer() == player) {
                        // Toast.makeText(this, "Congratulations, You Won...!", Toast.LENGTH_SHORT).show();
                        toastWin.show();

                    } else {
                        toastLose.show();
                        // Toast.makeText(this, "Player 1 Won...!", Toast.LENGTH_SHORT).show();
                    }
                    vib.vibrate(300);
                    pwin.start();
                    break;
                case 2:
                    if (gdb.getPlayer() == player) {
                        // Toast.makeText(this, "Congratulations, You Won...!", Toast.LENGTH_SHORT).show();
                        toastWin.show();
                    } else {
                        toastLose.show();
                        // Toast.makeText(this, "Player 2 Won...!", Toast.LENGTH_SHORT).show();
                    }
                    vib.vibrate(300);
                    pwin.start();
                    break;
            }
        }

        if (gdb.getPlayer() == 1) {

            vib.vibrate(30);
            txtPlayer1.setTextColor(green);
            txtPlayer2.setTextColor(red);
        } else if (gdb.getPlayer() == 2) {

            vib.vibrate(30);
            txtPlayer1.setTextColor(red);
            txtPlayer2.setTextColor(green);
        } else {

            txtPlayer1.setTextColor(red);
            txtPlayer2.setTextColor(red);
        }
        parseRTDB(gdb);
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.w("TicTacToe Google RTDB:", "Failed to read value." + error.toException());
    }

    @Override
    public void onClick(View view) {

        int mark = 1;
        if (player == 1) {
            mark = 0;
        } else if (player == 2) {
            mark = 1;
        }

        switch (view.getId()) {

            case R.id.img00:
                setMark(0, 0, mark);
                break;
            case R.id.img01:
                setMark(0, 1, mark);
                break;
            case R.id.img02:
                setMark(0, 2, mark);
                break;
            case R.id.img10:
                setMark(1, 0, mark);
                break;
            case R.id.img11:
                setMark(1, 1, mark);
                break;
            case R.id.img12:
                setMark(1, 2, mark);
                break;
            case R.id.img20:
                setMark(2, 0, mark);
                break;
            case R.id.img21:
                setMark(2, 1, mark);
                break;
            case R.id.img22:
                setMark(2, 2, mark);
                break;
            default:
                Toast.makeText(this, "Warning: Unhandled OnClick Event", Toast.LENGTH_SHORT).show();
                break;
        }

    }

    private void initFirebase(String str) {

        layout1.setEnabled(true);
        database = FirebaseDatabase.getInstance();

        Toast.makeText(this, "grtdb:" + str, Toast.LENGTH_SHORT).show();
        myRef = database.getReferenceFromUrl("https://tictactoe-b607e.firebaseio.com/" + str.trim());
        resetGame();
        myRef.addValueEventListener(this);
    }

    private void parseRTDB(GoogleDB fdb) {

        img[0][0].setImageResource(GameUtil.getImgRes(fdb.getAa()));
        img[0][1].setImageResource(GameUtil.getImgRes(fdb.getAb()));
        img[0][2].setImageResource(GameUtil.getImgRes(fdb.getAc()));
        img[1][0].setImageResource(GameUtil.getImgRes(fdb.getBa()));
        img[1][1].setImageResource(GameUtil.getImgRes(fdb.getBb()));
        img[1][2].setImageResource(GameUtil.getImgRes(fdb.getBc()));
        img[2][0].setImageResource(GameUtil.getImgRes(fdb.getCa()));
        img[2][1].setImageResource(GameUtil.getImgRes(fdb.getCb()));
        img[2][2].setImageResource(GameUtil.getImgRes(fdb.getCc()));

        M[0][0] = fdb.getAa();
        M[0][1] = fdb.getAb();
        M[0][2] = fdb.getAc();
        M[1][0] = fdb.getBa();
        M[1][1] = fdb.getBb();
        M[1][2] = fdb.getBc();
        M[2][0] = fdb.getCa();
        M[2][1] = fdb.getCb();
        M[2][2] = fdb.getCc();
    }

    private void setMark(int x, int y, int mark) {

        if (gdb.getPlayer() != player) {
            return;
        }
        int m = M[x][y];
        if (m == -1) {
            M[x][y] = mark;
            img[x][y].setImageResource(GameUtil.getImgRes(mark));
            img[x][y].startAnimation(animMark);
            mp.start();
            int p = checkGame();
            gdb.setWon(p);
            reflectToRTDB(1);
            if (p < 0) {
                // continue
            } else if (p == 0) {
                gameDraw();
            } else {
                gameWon(p);
            }
        } else {
            // Already Marked
        }
    }

    private void reflectToRTDB(int isUIThread) {

        if (gdb.getWon() < 0) {
            if (gdb.getPlayer() == 1) {
                gdb.setPlayer(2);
            } else {
                gdb.setPlayer(1);
            }
        }

        gdb.setAa(M[0][0]);
        gdb.setAb(M[0][1]);
        gdb.setAc(M[0][2]);
        gdb.setBa(M[1][0]);
        gdb.setBb(M[1][1]);
        gdb.setBc(M[1][2]);
        gdb.setCa(M[2][0]);
        gdb.setCb(M[2][1]);
        gdb.setCc(M[2][2]);
        myRef.setValue(gdb);

        if (isUIThread == 0) {
            return; //Return if not on UI Thread.
        }

        // Continue further for UI work.
        if (gdb.getPlayer() == 1) {
            txtPlayer1.setTextColor(green);
            txtPlayer2.setTextColor(red);

        } else if (gdb.getPlayer() == 2) {
            txtPlayer1.setTextColor(red);
            txtPlayer2.setTextColor(green);
        }
    }

    private void gameWon(int player) {

        myScore++;
        txtScore.setText("Score: " + myScore);
        gdb.setWon(player);
        newGame();

    }

    private void gameDraw() {

        gdb.setWon(0);
        newGame();
    }

    private void newGame() {

        layout1.setEnabled(true);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                MainActivity.this.resetGame();
            }
        }, 1000);
    }

    private void resetGame() {

        gdb.setAa(-1);
        gdb.setAb(-1);
        gdb.setAc(-1);
        gdb.setBa(-1);
        gdb.setBb(-1);
        gdb.setBc(-1);
        gdb.setCa(-1);
        gdb.setCb(-1);
        gdb.setCc(-1); // 0=o, 1=x, -1=nil (mark on board)
        if (gdb.getWon() > -1) {
            gdb.setPlayer(gdb.getPlayer());// 1 = player1 and 2 = player 2
        } else {
            gdb.setPlayer(1);
        }
        gdb.setWon(-1);  // -1 = Nothing, 0 = Draw, 1 = player 1 and 2 = player 2
        try {
            myRef.setValue(gdb);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        M = new int[][]{
                {-1, -1, -1},
                {-1, -1, -1},
                {-1, -1, -1}
        };

        img[0][0].setImageResource(android.R.color.transparent);
        img[0][1].setImageResource(android.R.color.transparent);
        img[0][2].setImageResource(android.R.color.transparent);
        img[1][0].setImageResource(android.R.color.transparent);
        img[1][1].setImageResource(android.R.color.transparent);
        img[1][2].setImageResource(android.R.color.transparent);
        img[2][0].setImageResource(android.R.color.transparent);
        img[2][1].setImageResource(android.R.color.transparent);
        img[2][2].setImageResource(android.R.color.transparent);

        img[0][0].setBackgroundColor(Color.TRANSPARENT);
        img[0][1].setBackgroundColor(Color.TRANSPARENT);
        img[0][2].setBackgroundColor(Color.TRANSPARENT);
        img[1][0].setBackgroundColor(Color.TRANSPARENT);
        img[1][1].setBackgroundColor(Color.TRANSPARENT);
        img[1][2].setBackgroundColor(Color.TRANSPARENT);
        img[2][0].setBackgroundColor(Color.TRANSPARENT);
        img[2][1].setBackgroundColor(Color.TRANSPARENT);
        img[2][2].setBackgroundColor(Color.TRANSPARENT);

        img[0][0].clearAnimation();
        img[0][1].clearAnimation();
        img[0][2].clearAnimation();
        img[1][0].clearAnimation();
        img[1][1].clearAnimation();
        img[1][2].clearAnimation();
        img[2][0].clearAnimation();
        img[2][1].clearAnimation();
        img[2][2].clearAnimation();

        txtPlayer1.clearAnimation();
        txtPlayer2.clearAnimation();

        layout1.setEnabled(true);
    }

    private int checkGame() {

        if (M[0][0] == M[1][1] && M[1][1] == M[2][2] & M[2][2] != -1) {
            img[0][0].setBackgroundResource(R.drawable.pattern_win);
            img[1][1].setBackgroundResource(R.drawable.pattern_win);
            img[2][2].setBackgroundResource(R.drawable.pattern_win);
            return M[2][2] + 1; //Won

        } else if (M[2][0] == M[1][1] && M[1][1] == M[0][2] & M[0][2] != -1) {
            img[2][0].setBackgroundResource(R.drawable.pattern_win);
            img[1][1].setBackgroundResource(R.drawable.pattern_win);
            img[0][2].setBackgroundResource(R.drawable.pattern_win);
            return M[0][2] + 1; //Won

        }

        for (int i = 0; i < 3; i++) {
            if (M[i][0] == M[i][1] && M[i][1] == M[i][2] & M[i][2] != -1) {
                img[i][0].setBackgroundResource(R.drawable.pattern_win);
                img[i][1].setBackgroundResource(R.drawable.pattern_win);
                img[i][2].setBackgroundResource(R.drawable.pattern_win);
                return M[i][2] + 1; //Won

            } else if (M[0][i] == M[1][i] && M[1][i] == M[2][i] & M[2][i] != -1) {
                img[0][i].setBackgroundResource(R.drawable.pattern_win);
                img[1][i].setBackgroundResource(R.drawable.pattern_win);
                img[2][i].setBackgroundResource(R.drawable.pattern_win);
                return M[2][i] + 1; // Won
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (M[i][j] == -1) {
                    return -1;  // Nothing
                }
            }
        }

        return 0; //Draw
    }

    public void Player2Joined() {

        player = 2;
        initFirebase(Constants.friendMob.trim());
        gdb.setGameStatus(1);
        reflectToRTDB(0);
    }
}
