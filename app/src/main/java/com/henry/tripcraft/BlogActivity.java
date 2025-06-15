package com.henry.tripcraft;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class BlogActivity extends AppCompatActivity {

    private ImageView backButton;
    private View blogImagePlaceholder;
    private ImageView blogImageView; // New ImageView for actual images
    private TextView blogTitle;
    private TextView blogAuthor;
    private TextView blogDate;
    private TextView blogContent;
    private MaterialButton shareButton;
    private MaterialButton bookmarkButton;

    private String blogTitleText;
    private String blogContentText;
    private String blogAuthorText;
    private String blogDateText;
    private String blogImageColor;
    private int blogImageResourceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blog);

        // Initialize views
        initializeViews();

        // Get data from intent
        getBlogDataFromIntent();

        // Set up the blog content
        setupBlogContent();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        blogImagePlaceholder = findViewById(R.id.blogImagePlaceholder);
        blogImageView = findViewById(R.id.blogImageView);
        blogTitle = findViewById(R.id.blogTitle);
        blogAuthor = findViewById(R.id.blogAuthor);
        blogDate = findViewById(R.id.blogDate);
        blogContent = findViewById(R.id.blogContent);
        shareButton = findViewById(R.id.shareButton);
        bookmarkButton = findViewById(R.id.bookmarkButton);
    }

    private void getBlogDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            blogTitleText = intent.getStringExtra("blog_title");
            blogContentText = intent.getStringExtra("blog_content");
            blogAuthorText = intent.getStringExtra("blog_author");
            blogDateText = intent.getStringExtra("blog_date");
            blogImageColor = intent.getStringExtra("blog_image_color");
            blogImageResourceId = intent.getIntExtra("blog_image_resource", 0);
        }
    }

    private void setupBlogContent() {
        // Set title
        if (blogTitleText != null) {
            blogTitle.setText(blogTitleText);
        }

        // Set author
        if (blogAuthorText != null) {
            blogAuthor.setText("By " + blogAuthorText);
        }

        // Set date
        if (blogDateText != null) {
            blogDate.setText(blogDateText);
        }

        // Set content
        if (blogContentText != null) {
            blogContent.setText(blogContentText);
        }

        // Set image or color placeholder
        if (blogImageResourceId != 0) {
            // Show actual image
            blogImageView.setImageResource(blogImageResourceId);
            blogImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            blogImageView.setVisibility(View.VISIBLE);
            blogImagePlaceholder.setVisibility(View.GONE);
        } else {
            // Show color placeholder
            blogImageView.setVisibility(View.GONE);
            blogImagePlaceholder.setVisibility(View.VISIBLE);
            if (blogImageColor != null) {
                try {
                    blogImagePlaceholder.setBackgroundColor(Color.parseColor(blogImageColor));
                } catch (Exception e) {
                    blogImagePlaceholder.setBackgroundColor(Color.parseColor("#1565C0")); // Default color
                }
            }
        }
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Share button
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareBlog();
            }
        });

        // Bookmark button
        bookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookmarkBlog();
            }
        });
    }

    private void shareBlog() {
        if (blogTitleText != null && blogAuthorText != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");

            String shareText = "Check out this amazing travel blog: \"" + blogTitleText +
                    "\" by " + blogAuthorText +
                    "\n\nShared from TripCraft";

            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Travel Blog: " + blogTitleText);

            try {
                startActivity(Intent.createChooser(shareIntent, "Share Blog"));
            } catch (Exception e) {
                Toast.makeText(this, "Unable to share at this moment", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void bookmarkBlog() {
        // Here you would typically save to SharedPreferences, Room database, or Firebase
        // For now, we'll just show a success message

        Toast.makeText(this, "Blog bookmarked successfully!", Toast.LENGTH_SHORT).show();

        // You could implement actual bookmark functionality like this:
        /*
        SharedPreferences prefs = getSharedPreferences("Bookmarks", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Create a unique key for this blog
        String bookmarkKey = "bookmark_" + System.currentTimeMillis();

        // Save blog data as JSON or separate fields
        editor.putString(bookmarkKey + "_title", blogTitleText);
        editor.putString(bookmarkKey + "_author", blogAuthorText);
        editor.putString(bookmarkKey + "_date", blogDateText);
        editor.putString(bookmarkKey + "_content", blogContentText);
        editor.putString(bookmarkKey + "_color", blogImageColor);
        editor.putInt(bookmarkKey + "_image_resource", blogImageResourceId);

        editor.apply();
        */
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}