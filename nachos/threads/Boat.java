package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    static Lock l1;
    static Lock l2;
    static Lock l3;
    static Condition c1;
    static Condition c2;
    static Condition c3;
    
    
    static int Oahu ;
    static int Molokai ;
    static int whereIsTheBoat;
    static int howManyPeopleOnBoat;
    
    
    static int childrenOnOahu;
    static int adultOnOahu;
    static int childrenOnMolakai;
    static int adultOnMolakai;
    
    public static void selfTest()
    {
        BoatGrader b = new BoatGrader();
     

        System.out.println("\n ***Testing Boats with only 2 children***");
        begin(0, 2, b);
        
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
        
        
        Oahu = 0;
        Molokai = 1;
        
        childrenOnMolakai=0;
        childrenOnOahu= children;
        adultOnMolakai=0;
        adultOnOahu = adults;
        
        
        // Instantiate global variables here
        
        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.
        
        Runnable r = new Runnable() {
            public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();
        
    }
    
    static void AdultItinerary()
    {
        bg.initializeAdult();
        
        //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE.

        int place=0;

        l1.acquire();
        while (childrenOnMolakai == 0 || whereIsTheBoat == Molokai || howManyPeopleOnBoat !=0 ){
            c2.sleep();
        }
        howManyPeopleOnBoat=2;
        adultOnOahu--;
        
        
        bg.AdultRowToMolokai();
        
        whereIsTheBoat = Molokai;
        howManyPeopleOnBoat=0;
        AdultRideToMolokai++;
        c3.wake();
        l1.release();
        
        

        
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

        boolean isNotFinished = true;
        while (isNotFinished){
            l1.acquire();
            if (place==Oahu){
                while(childrenOnMolakai !=0 && adultOnOahu !=0){
                    c1.sleep();
                }
                myOrderOnBoat= howManyPeopleOnBoat+1;
                howManyPeopleOnBoat++;
                if (howManyPeopleOnBoat==1){
                    childrenOnOahu--;
                    childrenOnMolakai++;
                    bg.ChildrenRowToMolokai();
                    
                }
            }
        }
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
