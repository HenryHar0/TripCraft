package com.henry.tripcraft;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private TextView usernameText;
    private TextView sloganText;
    private BottomNavigationView bottomNavigationView;
    private RecyclerView blogsRecyclerView;
    private BlogAdapter blogAdapter;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        usernameText = findViewById(R.id.usernameText);
        sloganText = findViewById(R.id.sloganText);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        blogsRecyclerView = findViewById(R.id.blogsRecyclerView);

        // Set username from Firebase or SharedPreferences
        setUsername();

        // Setup blogs RecyclerView
        setupBlogsRecyclerView();

        // Setup bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Set home as selected by default
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.navigation_home) {
            // Already on home, do nothing or refresh
            return true;
        } else if (id == R.id.navigation_saved_plan) {
            // Navigate to SavedPlansActivity
            Intent intent = new Intent(MainActivity.this, SavedPlansActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.navigation_plus) {
            // Navigate to CityActivity
            Intent intent = new Intent(MainActivity.this, CityActivity.class);
            startActivityForResult(intent, 1);
            return true;
        } else if (id == R.id.navigation_ai) {
            // Navigate to RestaurantActivity
            Intent intent = new Intent(MainActivity.this, RestaurantActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.profileButton) {
            // Navigate to ProfileActivity
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure home is selected when returning to MainActivity
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            Intent intent = new Intent(MainActivity.this, SavedPlansActivity.class);
            startActivity(intent);
        }
    }

    private void setUsername() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Try SharedPreferences first
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String savedName = prefs.getString("displayName", null);

            System.out.println("USERNAME DEBUG: SharedPrefs name: " + savedName);
            System.out.println("USERNAME DEBUG: Firebase name: " + currentUser.getDisplayName());

            String displayName = "Guest";

            if (savedName != null && !savedName.isEmpty()) {
                displayName = savedName;
            } else if (currentUser.getDisplayName() != null &&
                    !currentUser.getDisplayName().isEmpty() &&
                    !currentUser.getDisplayName().equals("User Name")) {
                // Use Firebase name if it's not the default "User Name"
                displayName = currentUser.getDisplayName();
            } else {
                // Fall back to email username
                String email = currentUser.getEmail();
                if (email != null && !email.isEmpty()) {
                    displayName = email.split("@")[0];
                }
            }

            usernameText.setText("Welcome back, " + displayName + "!");
        } else {
            usernameText.setText("Welcome back, Guest!");
        }
    }

    private void setupBlogsRecyclerView() {
        blogsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<BlogPost> blogs = createSampleBlogs();
        blogAdapter = new BlogAdapter(blogs, new BlogAdapter.OnBlogClickListener() {
            @Override
            public void onBlogClick(BlogPost blog) {
                Intent intent = new Intent(MainActivity.this, BlogActivity.class);
                intent.putExtra("blog_title", blog.getTitle());
                intent.putExtra("blog_content", blog.getContent());
                intent.putExtra("blog_author", blog.getAuthor());
                intent.putExtra("blog_date", blog.getDate());
                intent.putExtra("blog_image_color", blog.getImageColor());
                intent.putExtra("blog_image_resource", blog.getImageResourceId()); // Add this line
                startActivity(intent);
            }
        });

        blogsRecyclerView.setAdapter(blogAdapter);
    }
    private List<BlogPost> createSampleBlogs() {
        List<BlogPost> blogs = new ArrayList<>();

        blogs.add(new BlogPost(
                "Complete Guide to Santorini Tourism",
                "Everything you need to know about visiting Greece's most famous island...",
                "Henry Harutyunyan",
                "June 15, 2025",
                "#1565C0",
                "Santorini, officially known as Thira, is a volcanic island in the Cyclades group of the Greek islands. Located approximately 200 kilometers southeast of mainland Greece, it attracts over 2 million visitors annually with its distinctive architecture and dramatic landscapes.\n\nThe island's unique geography results from a massive volcanic eruption around 3,600 years ago, creating the current crescent shape and the famous caldera. This geological formation is responsible for the island's black, red, and white beaches, each offering different mineral compositions and scenic experiences.\n\nKey attractions include the village of Oia, famous for its sunset views and traditional Cycladic architecture with white-washed buildings and blue domes. Fira, the capital, offers museums, restaurants, and cable car access to the old port. The archaeological site of Akrotiri provides insights into ancient Minoan civilization preserved by volcanic ash.\n\nTransportation options include the island's bus system connecting major villages, rental cars and ATVs for independent exploration, and organized tours for comprehensive sightseeing. The island's airport receives direct flights from major European cities during peak season.\n\nBest visiting times are late spring (May-June) and early fall (September-October) when temperatures are pleasant and crowds are smaller. Summer months offer the warmest weather but also the highest prices and largest tourist numbers.\n\nAccommodation ranges from luxury cave hotels built into cliffsides to budget-friendly guesthouses inland. Booking well in advance is essential, especially for sunset-view properties.",
                R.drawable.santorini_blog
        ));

        blogs.add(new BlogPost(
                "Japan Travel Guide: Culture, Etiquette & Planning",
                "Essential information for understanding and navigating Japanese culture...",
                "Henry Harutyunyan",
                "June 15, 2025",
                "#00ACC1",
                "Japan operates on unique cultural principles that visitors should understand for respectful and successful travel experiences. The concept of 'omotenashi' (hospitality) means service is provided without expectation of reward, creating exceptionally high standards in all customer interactions.\n\nTransportation systems are renowned for efficiency and punctuality. The JR Pass provides unlimited travel on most trains for tourists, including shinkansen (bullet trains) except the fastest Nozomi services. IC cards like Suica or Pasmo work for local transport in major cities and can be used for small purchases.\n\nBowing remains an important gesture, though tourists aren't expected to master complex protocols. A slight bow shows respect and appreciation. Removing shoes is required when entering homes, traditional restaurants, temples, and some accommodations.\n\nCash remains king in Japan despite technological advancement. Many establishments don't accept credit cards, and tipping is unnecessary and can cause confusion. ATMs at 7-Eleven stores and post offices typically accept foreign cards.\n\nAccommodation options include traditional ryokan inns with tatami mats, futon beds, and often include elaborate kaiseki dinners. Business hotels offer compact, efficient rooms with modern amenities. Capsule hotels provide budget-friendly individual sleeping pods.\n\nLanguage barriers are manageable with translation apps, though learning basic phrases like 'arigatou gozaimasu' (thank you) and 'sumimasen' (excuse me) demonstrates cultural respect.\n\nSeasonal considerations are crucial: spring brings cherry blossoms and crowds, summer offers festivals but intense humidity, autumn provides comfortable weather and fall colors, while winter enables winter sports and fewer tourists.",
                R.drawable.japan_blog
        ));

        blogs.add(new BlogPost(
                "Patagonia Hiking Guide: Preparation & Routes",
                "Complete information for planning safe adventures in South America's wilderness...",
                "Henry Harutyunyan",
                "June 15, 2025",
                "#2E7D32",
                "Patagonia spans approximately 1 million square kilometers across southern Argentina and Chile, featuring some of the world's most challenging and rewarding hiking terrain. The region's unpredictable weather requires careful preparation and flexible planning.\n\nTorres del Paine National Park in Chile offers the famous 'W' trek (4-5 days) and the complete circuit (8-10 days). The park requires advance reservations for camping and refugios, especially during peak season (December-March). Weather can change rapidly from sunshine to snow regardless of season.\n\nArgentina's Los Glaciares National Park includes Mount Fitz Roy and Cerro Torre areas. The town of El Chaltén serves as the trekking base with various day hikes and multi-day options. No permits are required, but weather windows for summit attempts are limited.\n\nEssential gear includes waterproof and windproof outer layers, insulating mid-layers, and moisture-wicking base layers. Weather can range from freezing to hot within hours. Quality hiking boots, gaiters, and trekking poles are crucial for varied terrain.\n\nPhysical preparation should begin months in advance. Training should include cardio endurance, leg strength, and hiking with weighted packs. Altitude acclimatization isn't typically necessary as most trails remain below 2,000 meters.\n\nSupply considerations include purchasing gear in Buenos Aires or Santiago before traveling to remote areas where selection is limited and prices are high. Many refugios provide meals, but carrying emergency food is essential.\n\nSafety protocols include registering with park authorities, carrying emergency communication devices in remote areas, and understanding river crossing techniques as bridges may be damaged by weather.",
                R.drawable.patagonia_blog
        ));

        blogs.add(new BlogPost(
                "Vietnamese Cuisine: Regional Specialties & Food Culture",
                "Understanding Vietnam's diverse culinary traditions and regional variations...",
                "Henry Harutyunyan",
                "June 15, 2025",
                "#FF9800",
                "Vietnamese cuisine reflects the country's geography, history, and cultural influences through distinct regional variations. The cuisine emphasizes fresh herbs, balanced flavors, and minimal use of oil, creating healthy and flavorful dishes.\n\nNorthern cuisine, centered around Hanoi, features subtle flavors with less sugar and spice. Pho originated here with clear, refined broths. Bun cha, grilled pork with noodles, represents the region's preference for grilled meats. Black pepper is the primary spice, reflecting Chinese culinary influence.\n\nCentral Vietnam, including Hue and Hoi An, offers the most complex and spicy dishes. The former imperial capital's royal cuisine features intricate presentation and bold flavors. Cao lau noodles are unique to Hoi An, made with water from specific local wells.\n\nSouthern Vietnamese food incorporates more sugar, coconut milk, and diverse vegetables due to the Mekong Delta's agricultural abundance. Pho here features sweeter broths with more herbs and bean sprouts. Cambodian and Thai influences appear in curry dishes and tropical fruit usage.\n\nStreet food culture operates on specific timing: pho vendors typically serve from early morning until mid-day, while bun bo hue appears in afternoons. Evening brings grilled foods and beer culture.\n\nEssential condiments include nuoc mam (fish sauce), the cornerstone of Vietnamese flavor profiles. Fresh herbs like cilantro, mint, and Thai basil accompany most dishes. Lime juice and chili provide additional flavor customization.\n\nFood safety practices include choosing busy vendors with high turnover, avoiding pre-cut fruits in hot weather, and ensuring meat is thoroughly cooked. Bottled or boiled water is recommended for sensitive stomachs.",
                R.drawable.vietnam_blog
        ));

        blogs.add(new BlogPost(
                "Digital Nomad Guide: Bali Infrastructure & Costs",
                "Practical information for remote workers considering Bali as a base...",
                "Henry Harutyunyan",
                "June 15, 2025",
                "#E91E63",
                "Bali has developed significant infrastructure supporting digital nomads and remote workers, though challenges remain. Internet connectivity varies dramatically by location, with Canggu, Ubud, and Sanur offering the most reliable high-speed options.\n\nCo-working spaces provide professional environments with backup power and premium internet. Major options include Hubud in Ubud, Dojo Bali in Canggu, and BWork in Sanur. Monthly memberships range from $100-200 USD and include networking events and community access.\n\nVisa requirements for longer stays include the B211 visit visa (30 days, extendable to 60) or the B213 cultural/business visa (60 days, multiple extensions possible). The Indonesian government is developing specific digital nomad visa programs.\n\nCost of living varies significantly by lifestyle and location. Basic monthly budgets start around $800-1000 USD including accommodation, food, and transportation. Mid-range lifestyles cost $1500-2500 USD monthly, while luxury options exceed $3000 USD.\n\nAccommodation options include monthly villa rentals ($300-800 USD), co-living spaces with built-in communities ($400-600 USD), and hotels with extended stay rates. Popular areas for nomads include Ubud for cultural immersion, Canggu for surfing and nightlife, and Sanur for families and quieter environments.\n\nTransportation relies primarily on motorbikes (rental: $50-70 USD monthly) or ride-sharing apps like Gojek and Grab. International driving permits are required for legal motorbike operation.\n\nHealth considerations include comprehensive travel insurance, access to international-standard medical facilities in Denpasar and Ubud, and preventive measures for tropical diseases. Water quality varies, with bottled water recommended for drinking.",
                R.drawable.bali_blog
        ));

        return blogs;
    }
}