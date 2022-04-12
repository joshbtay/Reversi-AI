import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.math.*;
import java.text.*;

public class RandomGuy {

    public Socket s;
	public BufferedReader sin;
	public PrintWriter sout;
    int [][] priority =
            {
                {99, -8, 8, 6, 6, 8, -8, 99},
                {-8, -24, -4, -3, -3, -4, -24, -8 },
                {8, -4, 7, 4, 4, 7, -4, 8},
                {6, -3, 4, 0, 0, 4, -3, 6},
                {6, -3, 4, 0, 0, 4, -3, 6},
                {8, -4, 7, 4, 4, 7, -4, 8},
                {-8, -24, -4, -3, -3, -4, -24, -8 },
                {99, -8, 8, 6, 6, 8, -8, 99}
            };
    double t1, t2;
    int me;
    int them;
    int state[][] = new int[8][8];
    int turn = -1;
    int round;
    int MAX = Integer.MAX_VALUE;
    int MIN = Integer.MIN_VALUE;
    double CORNER = 100;
    double MOBILITY =.4;
    double STABILITY = 8;
    double EXP = 1.4;
    double COUNT = 2;
    int DEPTH = 9;
    class Move implements Comparable<Move>{
        int r;
        int c;

        public Move(int r, int c) {
            this.r=r;
            this.c=c;
        }

        @Override
        public int compareTo(Move o) {
            return (Integer.compare(priority[o.r][o.c], priority[r][c]));
        }
    }
    // main function that (1) establishes a connection with the server, and then plays whenever it is this player's turn
    public RandomGuy(int _me, String host) {
        me = _me;
        them = me % 2 + 1;
        initClient(host);

        int myMove;

        while (true) {
            readMessage();

            if (turn == me) {
                ArrayList<Move> validMoves = getValidMoves(me, state);
                myMove = move(validMoves);
                String sel = validMoves.get(myMove).r+ "\n" + validMoves.get(myMove).c;
                sout.println(sel);
            }
        }
    }

    private double evaluate(int[][] s, int gameDepth){
        double count = 0;
        double corn = 0;
        double perm = 0;
        double other = 0;
        for (int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                if (s[i][j] == me){
                    count += 1;
                }
                else if(s[i][j] == them) {
                    other += 1;
                }

            }
        }
        if (count == 0){
            return MIN;
        }
        if(other == 0){
            return MAX;
        }
        count -= other;
        if(gameDepth == 64){
            return count * 300;
        }

        boolean [][] stable = new boolean[8][8];
        if(s[0][0] + s[0][7] + s[7][0] + s[7][7] > 0){
            for(int i = 0; i < 2; i++){
                for(int j = 0; j < 2; j++){
                    int r = i * 7;
                    int c = j * 7;
                    int dx = i * -2 + 1;
                    int dy = j * -2 + 1;
                    Deque<tup> q = new LinkedList<>();
                    if (s[r][c] == me){
                        corn += CORNER;
                        stable[r][c] = true;
                        if(s[r+dx][c] == me)
                            q.add(new tup(r+dx,c));
                        if (s[r][c+dy] == me)
                            q.add(new tup(r,c+dy));
                    }else if(s[r][c] == them){
                        corn -= CORNER;
                        stable[r][c] = true;
                        if(s[r+dx][c] == them)
                            q.add(new tup(r+dx,c));
                        if (s[r][c+dy] == them)
                            q.add(new tup(r,c+dy));
                    }
                    while(!q.isEmpty()){
                        tup t = q.pollFirst();
                        r = t.r;
                        c = t.c;
                        if (stable[r][c]) continue;
                        if(isStable(r, c, s)) {
                            stable[r][c] = true;
                            if(s[r][c] == me) perm += 1;
                            else perm -= 1;
                            int rp = r+dx;
                            int cp = c+dy;
                            if(bounded(rp, c) && s[r][c] == s[rp][c] && !stable[rp][c])
                                q.add(new tup(rp,c));
                            if(bounded(r,cp) && s[r][c] == s[r][cp] && !stable[r][cp])
                                q.add(new tup(r,cp));
                        }
                    }
                }
            }
        }
        count *= COUNT * gameDepth / 64;
        count += corn;
        count += perm * STABILITY;
        return count;
    }

    tup[][] pairs ={
            {new tup(-1, 0), new tup(1, 0)},
            {new tup(0, -1), new tup(0, 1)},
            {new tup(-1, -1), new tup(1, 1)},
            {new tup(1, -1), new tup(1, -1)},
    };
    private boolean isStable(int r, int c, int[][] s) {
        for(tup[] pair : pairs){
            int p = pair[0].r+r,q = pair[0].c+c,x = pair[1].r+r,y = pair[1].c+c;
            if (!bounded(p,q) || !bounded(x,y)) continue;
            if (s[p][q] != s[r][c] && s[x][y] != s[r][c]) return false;
        }
        return true;
    }

    private class tup{
        int r;
        int c;

        public tup(int r, int c) {
            this.r = r;
            this.c = c;
        }

    }

    private void copy(int[][] bs, int[][] s){
        for (int j = 0; j < 8; j++) {
            System.arraycopy(bs[j], 0, s[j], 0, 8);
        }
    }
    private double minimax(int[][] st, int depth, double a, double b, int turn, int gameDepth){
        if (depth == 0) return evaluate(st, gameDepth);
        ArrayList<Move> validMoves = getValidMoves(turn, st);
        if (validMoves.isEmpty()) return evaluate(st, gameDepth);
        int[][] s = new int[8][8];
        double val;
        if (turn == me){
            val=MIN;
            for (Move i : validMoves){
                copy(st, s);
                make_move(s, i.r, i.c, turn);
                val = Math.max(val, minimax(s, depth - 1, a, b, them, gameDepth + 1));
                if (val >= b) break;
                a = Math.max(a, val);
            }
        }else{
            val=MAX;
            double subtract = (Math.pow(validMoves.size(), EXP) * MOBILITY) * (1 - (double) gameDepth / 64);
            for (Move i : validMoves){
                copy(st, s);
                make_move(s, i.r, i.c,turn);
                val = Math.min(val, minimax(s, depth - 1, a, b, me, gameDepth + 1) - subtract);
                if (val <= a) break;
                b = Math.min(b, val);
            }
        }
        return val;
    }
    // You should modify this function
    // validMoves is a list of valid locations that you could place your "stone" on this turn
    // Note that "state" is a global variable 2D list that shows the state of the game
    private int move(ArrayList<Move> validMoves) {
        // just move randomly for now
        if (validMoves.size() <= 1){
            return 0;
        }
        double best = MIN;
        int best_index = -1;
        int[][] s = new int[8][8];
        double a = MIN;
        double b = MAX;
        for (int i = 0; i < validMoves.size(); i++) {
            int row = validMoves.get(i).r;
            int col = validMoves.get(i).c;
            copy(state, s);
            make_move(s, row, col, me);
            double val = minimax(s, DEPTH, a, b, them, round + 1);
            a = Math.max(val, a);
            System.out.println("Move [" +row + ", " + col + "] has score "+ val);
            if(best < val){
                best = val;
                best_index = i;
            }
        }
        return best_index;
    }
    private boolean bounded(int r, int c){
        return 0 <= r && r < 8 && 0 <= c && c < 8;
    }

    private void make_move(int[][] st, int row, int col, int move){
        assert st[row][col] == 0;
        st[row][col] = move;
        for(int i = -1; i < 2; i++){
            for (int j = -1; j < 2; j++){
                if(i == 0 && 0== j) continue;
                boolean flip = false;
                int count = 0;
                int r = row + i;
                int c = col + j;
                while(bounded(r,c)){
                    if(st[r][c] == 0) break;
                    if (st[r][c] == move){
                        flip = true;
                        break;
                    }
                    r += i;
                    c += j;
                    count += 1;
                }
                if (flip){
                    while (count > 0){
                        count -= 1;
                        r -= i;
                        c -= j;
                        st[r][c] = move;
                    }
                }
            }
        }
    }

    // generates the set of valid moves for the player; returns a list of valid moves (validMoves)
    private ArrayList<Move> getValidMoves(int turn, int st[][]) {
        int i, j;
        ArrayList<Move> validMoves = new ArrayList<>();
        if (round < 4) {
            if (st[3][3] == 0) {
                validMoves.add(new Move(3,3));
            }
            if (st[3][4] == 0) {
                validMoves.add(new Move(3, 4));
            }
            if (st[4][3] == 0) {
                validMoves.add(new Move(4,3));
            }
            if (st[4][4] == 0) {
                validMoves.add(new Move(4, 4));
            }
        }
        else {
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    if (st[i][j] == 0) {
                        if (couldBe(st, i, j, turn)) {
                            validMoves.add(new Move(i, j));
                        }
                    }
                }
            }
        }
        Collections.sort(validMoves);
        return validMoves;
    }

    private boolean checkDirection(int st[][], int row, int col, int incx, int incy, int turn) {
        int sequence[] = new int[7];
        int seqLen;
        int i, r, c;

        seqLen = 0;
        for (i = 1; i < 8; i++) {
            r = row+incy*i;
            c = col+incx*i;

            if ((r < 0) || (r > 7) || (c < 0) || (c > 7))
                break;

            sequence[seqLen] = st[r][c];
            seqLen++;
        }

        int count = 0;
        for (i = 0; i < seqLen; i++) {
            if (turn == 1) {
                if (sequence[i] == 2)
                    count ++;
                else {
                    if ((sequence[i] == 1) && (count > 0))
                        return true;
                    break;
                }
            }
            else {
                if (sequence[i] == 1)
                    count ++;
                else {
                    if ((sequence[i] == 2) && (count > 0))
                        return true;
                    break;
                }
            }
        }

        return false;
    }

    private boolean couldBe(int st[][], int row, int col, int turn) {
        int incx, incy;

        for (incx = -1; incx < 2; incx++) {
            for (incy = -1; incy < 2; incy++) {
                if ((incx == 0) && (incy == 0))
                    continue;

                if (checkDirection(st, row, col, incx, incy, turn))
                    return true;
            }
        }

        return false;
    }

    public void readMessage() {
        int i, j;
        try {
            turn = Integer.parseInt(sin.readLine());
            System.out.println(turn);
            if (turn == -999) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }

                System.exit(1);
            }

            round = Integer.parseInt(sin.readLine());
            t1 = Double.parseDouble(sin.readLine());
            t2 = Double.parseDouble(sin.readLine());
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    state[i][j] = Integer.parseInt(sin.readLine());
                }
            }
            sin.readLine();
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
    }

    public void initClient(String host) {
        int portNumber = 3333+me;

        try {
			s = new Socket(host, portNumber);
            sout = new PrintWriter(s.getOutputStream(), true);
			sin = new BufferedReader(new InputStreamReader(s.getInputStream()));

            String info = sin.readLine();
            System.out.println(info);
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
    }


    // compile on your machine: javac *.java
    // call: java RandomGuy [ipaddress] [player_number]
    //   ipaddress is the ipaddress on the computer the server was launched on.  Enter "localhost" if it is on the same computer
    //   player_number is 1 (for the black player) and 2 (for the white player)
    public static void main(String args[]) {
        new RandomGuy(Integer.parseInt(args[1]), args[0]);
    }
}
