/*===============================================================*
 *  File: SWP.java                                               *
 *                                                               *
 *  This class implements the sliding window protocol            *
 *  Used by VMach class                          *
 *  Uses the following classes: SWE, Packet, PFrame, PEvent,     *
 *                                                               *
 *  Author: Professor SUN Chengzheng                             *
 *          School of Computer Engineering                       *
 *          Nanyang Technological University                     *
 *          Singapore 639798                                     *
 *===============================================================*/
import java.util.Timer;
import java.util.TimerTask;


public class SWP {

/*========================================================================*
 the following are provided, do not change them!!
 *========================================================================*/
   //the following are protocol constants.
   public static final int MAX_SEQ = 7; 
   public static final int NR_BUFS = (MAX_SEQ + 1)/2;

   // the following are protocol variables
   private int oldest_frame = 0;
   private PEvent event = new PEvent();  
   private Packet out_buf[] = new Packet[NR_BUFS];

   //the following are used for simulation purpose only
   private SWE swe = null;
   private String sid = null;  

   //Constructor
   public SWP(SWE sw, String s){
      swe = sw;
      sid = s;
   }

   //the following methods are all protocol related
   private void init(){
      for (int i = 0; i < NR_BUFS; i++){
         out_buf[i] = new Packet();
      }
   }

   private void wait_for_event(PEvent e){
      swe.wait_for_event(e); //may be blocked
      oldest_frame = e.seq;  //set timeout frame seq
   }

   private void enable_network_layer(int nr_of_bufs) {
      //network layer is permitted to send if credit is available
      swe.grant_credit(nr_of_bufs);
   }

   private void from_network_layer(Packet p) {
      swe.from_network_layer(p);
   }

   private void to_network_layer(Packet packet) {
      swe.to_network_layer(packet);
   }

   private void to_physical_layer(PFrame fm)  {
      System.out.println("SWP: Sending frame: seq = " + fm.seq + 
                " ack = " + fm.ack + " kind = " + 
                PFrame.KIND[fm.kind] + " info = " + fm.info.data );
      System.out.flush();
      swe.to_physical_layer(fm);
   }

   private void from_physical_layer(PFrame fm) {
      PFrame fm1 = swe.from_physical_layer(); 
      fm.kind = fm1.kind;
      fm.seq = fm1.seq; 
      fm.ack = fm1.ack;
      fm.info = fm1.info;
   }


/*===========================================================================*
    implement your Protocol Variables and Methods below: 
 *==========================================================================*/
   private Packet in_buf[] = new Packet[NR_BUFS];
   private boolean arrived[] = new boolean[NR_BUFS]; //inbound bit map
   private boolean no_nak = true; //no negative ack sent out initially
   private Timer timer[] = new Timer[NR_BUFS];
   private Timer ack_timer = new Timer();

   static boolean between(int a, int b, int c){
      return (((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ((b < c) && (c < a)));
   }

   private int inc(int seq_num){
      // circularly increse seq_num
      return (seq_num + 1) % (MAX_SEQ + 1);
   }

   void send_frame(int fk, int frame_num, int frame_expected, Packet buffer[]){
      //construct and send a data/ack/nak frame
      PFrame s = new PFrame(); //scratch variable
      s.kind = fk; //kind = {data, ack, nak}

      if (fk == PFrame.DATA)
         s.info = buffer[frame_num % NR_BUFS];

      s.seq = frame_num;
      s.ack = (frame_expected + MAX_SEQ) % (MAX_SEQ + 1); //piggyback ack

      if (fk == PFrame.NAK){
         no_nak = false; //one nak per frame
      }

      to_physical_layer(s);

      if (fk == PFrame.DATA){
         start_timer(frame_num);
      }

      stop_ack_timer(); //no need for separate ack frame i.e. piggybacked

   }

   public void protocol6() {
      int ack_expected = 0; //lower edge of sender's window
      int next_frame_to_send = 0; //upper edge of sender's window
      int frame_expected = 0; //lower edge of receiver's window
      int too_far = NR_BUFS; //upper edge of receiver's window
      PFrame r = new PFrame(); //scratch variable

      for (int i=0; i < NR_BUFS; i++){
         in_buf[i] = new Packet();
         arrived[i] = false;
      }

      init();

      enable_network_layer(NR_BUFS); //to grant the number of creditnr credits to the network layer, so that the network layer can generate the number of creditnr new packets

      while(true) {   
         wait_for_event(event);
         switch(event.type) {
            case (PEvent.NETWORK_LAYER_READY): //accept, save, and transmit a frame
               from_network_layer(out_buf[next_frame_to_send % NR_BUFS]); //fetch new packet
               send_frame(PFrame.DATA, next_frame_to_send, frame_expected, out_buf); //construct frame and send to physical layer
               next_frame_to_send = inc(next_frame_to_send); // increase upper window edge
               break; 
            case (PEvent.FRAME_ARRIVAL): //a valid or control frame has arrived
               from_physical_layer(r); //fetch incoming frame
               if (r.kind == PFrame.DATA){
                  //undamaged frame arrived
                  if ((r.seq != frame_expected) && no_nak){
                     send_frame(PFrame.NAK, 0, frame_expected, out_buf);
                  } else {
                     start_ack_timer();
                  }

                  if (between(frame_expected, r.seq, too_far) && (arrived[r.seq % NR_BUFS] == false)){
                     //frames may be accepted in any order
                     arrived[r.seq % NR_BUFS] = true; //mark buffer as full
                     in_buf[r.seq % NR_BUFS] = r.info; //insert data into buffer
                     while(arrived[frame_expected % NR_BUFS]){
                        //pass frames and advance window
                        to_network_layer(in_buf[frame_expected % NR_BUFS]);
                        no_nak = true;
                        arrived[frame_expected % NR_BUFS] = false;
                        frame_expected = inc(frame_expected);
                        too_far = inc(too_far);
                        start_ack_timer(); //to see if a separate ack is needed
                     }
                  }
               }

               //if received frame is NAK
               if ((r.kind == PFrame.NAK) && between(ack_expected, (r.ack + 1) % (MAX_SEQ + 1), next_frame_to_send)){
                  //resend the frame as the frame is lost
                  send_frame(PFrame.DATA, (r.ack + 1) % (MAX_SEQ + 1), frame_expected, out_buf);
               }               
               // Basically, when we receive r.ack, all ack between that ack_expected and r.ack
               // is assumed to be received too and hence, we can grant credit for the next packet transmission
               while (between(ack_expected, r.ack, next_frame_to_send)){
                  stop_timer(ack_expected % NR_BUFS); //frame arrived intact
                  ack_expected = inc(ack_expected); //increase lower edge of sender's window
                  enable_network_layer(1); //suspect losing frame is frame is not in order
               }
               break;     
            case (PEvent.CKSUM_ERR): //damaged frame
               if (no_nak) {
                  send_frame(PFrame.NAK, 0, frame_expected, out_buf);
               }
               break;  
            case (PEvent.TIMEOUT): //timeout
               send_frame(PFrame.DATA, oldest_frame, frame_expected, out_buf);
               break; 
            case (PEvent.ACK_TIMEOUT): //ack_timer expired, send ack without piggyback
               send_frame(PFrame.ACK, 0, frame_expected, out_buf);
               break; 
            default: 
               System.out.println("SWP: undefined event type = " + event.type); 
               System.out.flush();
         }
      }
   }

   /* Note: when start_timer() and stop_timer() are called, 
      the "seq" parameter must be the sequence number, rather 
      than the index of the timer array, 
      of the frame associated with this timer, 
   */

   private void start_timer(int seq) {
      stop_timer(seq); //to ensure that the timer for this frame is restarted
      timer[seq % NR_BUFS] = new Timer();
      timer[seq % NR_BUFS].schedule(new TimerTask(){
         @Override
         public void run() {
            swe.generate_timeout_event(seq);
         }
      }, 50);
   }

   private void stop_timer(int seq) {
      if (timer[seq % NR_BUFS] != null){
         timer[seq % NR_BUFS].cancel();
      }
   }

   private void start_ack_timer() {
      stop_ack_timer();  //to ensure that the timer for this frame is restarted
      ack_timer = new Timer();
      ack_timer.schedule(new TimerTask() {
         @Override
         public void run(){
            swe.generate_acktimeout_event();
         }
      }, 20);
   }

   private void stop_ack_timer() {
      if (ack_timer != null) {
         ack_timer.cancel();
      }
   }
}//End of class

/* Note: In class SWE, the following two public methods are available:
   . generate_acktimeout_event() and
   . generate_timeout_event(seqnr).

   To call these two methods (for implementing timers),
   the "swe" object should be referred as follows:
     swe.generate_acktimeout_event(), or
     swe.generate_timeout_event(seqnr).
*/


