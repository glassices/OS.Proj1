package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    static int counter;
    
    
    static Lock l1;
    static Condition c1;
    static Condition c2;
    static Condition c3;
    
    
    static int Oahu ;
    static int Molokai ;
    static int whereIsTheBoat;
    static int howManyPeopleOnBoat;
    
    
    static int childrenOnOahu;
    static int adultOnOahu;
    static int childrenOnMolokai;
    static int adultOnMolokai;
    static boolean isNotFinished;

    
    public static void selfTest()
    {
        BoatGrader b = new BoatGrader();
     
        
        System.out.println("\n ***Testing Boats with 4 children and 1 adults***");
        begin(1, 4, b);
        System.out.println("\n ***Testing Boats with 0 children and 2 adults***");
        begin(2, 0, b);
        System.out.println("\n ***Testing Boats with 1 children and 1 adults***");
        begin(1, 1, b);
        System.out.println("\n ***Testing Boats with 1 children and 0 adults***");
        begin(0, 1, b);
        System.out.println("\n ***Testing Boats with 0 children and 1 adults***");
        begin(1, 0, b);
        System.out.println("\n ***Testing Boats with 3 children and 0 adults***");
        begin(0, 3, b);
        
        for (int itt=0;itt<10;itt++){
            for (int jtt=0;jtt<10;jtt++){
                begin(itt, jtt, b);

            }
        }
        //	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        //  	begin(1, 2, b);
        
        //  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        //  	begin(3, 3, b);
    }
    
    public static void begin( int adults, int children, BoatGrader b )
    {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;
        
        
        l1 = new Lock();
        c1= new Condition(l1);
        c2= new Condition(l1);
        c3= new Condition(l1);
        counter=adults+children;
        
        Oahu = 0;
        Molokai = 1;
        
        childrenOnMolokai=0;
        childrenOnOahu= children;
        adultOnMolokai=0;
        adultOnOahu = adults;
        whereIsTheBoat = 0;
        isNotFinished = true;
        
        
        // Instantiate global variables here
        
        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.
        if (children>1){
            Runnable ra = new Runnable() {
                public void run() {
                    AdultItinerary();
                }
            };
            Runnable rc = new Runnable() {
                public void run() {
                    ChildItinerary();
                }
            };
            for(int i =0 ;i<adults;i++){
                KThread t = new KThread(ra);
                t.fork();
            }
            for(int i =0 ;i<children;i++){
                KThread t = new KThread(rc);
                t.fork();
            }
            while(counter>0){
                KThread.yield();
            }
        }else if(children == 1 && adults == 0){
            bg.ChildRowToMolokai();

        }else if(children == 0 && adults == 1){
            bg.AdultRowToMolokai();
        }else {
            System.out.println("not possible");
        }
        
    }
    
    static void AdultItinerary()
    {
        bg.initializeAdult();
        
        //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE.

        int place=0;

        l1.acquire();
        while (childrenOnMolokai == 0 || whereIsTheBoat == Molokai || howManyPeopleOnBoat !=0 ){
            c1.wake();
            c2.sleep();
        }
        howManyPeopleOnBoat=2;
        adultOnOahu--;
        
        
        bg.AdultRowToMolokai();
        
        whereIsTheBoat = Molokai;
        howManyPeopleOnBoat=0;
        adultOnMolokai++;
        c3.wake();
        l1.release();
        
        counter--;

        
        /* This is where you should put your solutions. Make calls
         to the BoatGrader to show that it is synchronized. For
         example:
         bg.AdultRowToMolokai();
         indicates that an adult has rowed the boat across to Molokai
         */
    }
    
    static void ChildItinerary()
    {
        bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE.
        int place=0;

        l1.acquire();
        while (isNotFinished){
            if (isNotFinished){
                if (place==Oahu){
                    while((childrenOnMolokai !=0 && adultOnOahu !=0 && howManyPeopleOnBoat ==0) || whereIsTheBoat == Molokai){

                        c2.wake();
                        c1.sleep();
                        
                    }
                    if (isNotFinished){
                        
                        int myOrderOnBoat= howManyPeopleOnBoat+1;
                        howManyPeopleOnBoat++;
                        if (myOrderOnBoat==1){
                            childrenOnOahu--;
                            childrenOnMolokai++;
                            bg.ChildRowToMolokai();
                            place= Molokai;
                            c1.wake();
                            c3.sleep();
                        }else if (isNotFinished){
                            childrenOnOahu--;
                            childrenOnMolokai++;
                            bg.ChildRideToMolokai();
                            place=Molokai;
                            whereIsTheBoat= Molokai;
                            howManyPeopleOnBoat = 0;
                            if(childrenOnOahu==0 && adultOnOahu ==0){
                                isNotFinished=false;
                                c3.wake();
                            }else{
                                c3.wake();
                                c3.sleep();
                            }
                        }
                    }
                }
                else{ //place = Molokai
                    while( whereIsTheBoat == Oahu){
                        c1.sleep();
                    }
                    childrenOnMolokai--;
                    childrenOnOahu++;
                    bg.ChildRowToOahu();
                    whereIsTheBoat=Oahu;
                    place= Oahu;
                    if (adultOnOahu==0){
                    c1.wake();
                    }else
                    {
                        c2.wake();
                    }
                    c1.sleep();
                    
                    
                }
            }
        }
        c3.wake();
        counter--;
        l1.release();
    }
    
    static void SampleItinerary()
    {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }
    
}
//problem 6 solved
