package com.henry.tripcraft;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.BlogViewHolder> {

    private List<BlogPost> blogs;
    private OnBlogClickListener listener;

    public interface OnBlogClickListener {
        void onBlogClick(BlogPost blog);
    }

    public BlogAdapter(List<BlogPost> blogs, OnBlogClickListener listener) {
        this.blogs = blogs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blog, parent, false);
        return new BlogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlogViewHolder holder, int position) {
        BlogPost blog = blogs.get(position);
        holder.bind(blog, listener);
    }

    @Override
    public int getItemCount() {
        return blogs.size();
    }

    static class BlogViewHolder extends RecyclerView.ViewHolder {
        private ImageView blogImage;
        private TextView blogTitle;
        private TextView blogDescription;
        private TextView blogAuthor;
        private TextView blogDate;

        public BlogViewHolder(@NonNull View itemView) {
            super(itemView);
            blogImage = itemView.findViewById(R.id.blogImage);
            blogTitle = itemView.findViewById(R.id.blogTitle);
            blogDescription = itemView.findViewById(R.id.blogDescription);
            blogAuthor = itemView.findViewById(R.id.blogAuthor);
            blogDate = itemView.findViewById(R.id.blogDate);
        }

        public void bind(BlogPost blog, OnBlogClickListener listener) {
            blogTitle.setText(blog.getTitle());
            blogDescription.setText(blog.getDescription());
            blogAuthor.setText(blog.getAuthor());
            blogDate.setText(blog.getDate());

            // Set image if available, otherwise use color background
            if (blog.getImageResourceId() != 0) {
                blogImage.setImageResource(blog.getImageResourceId());
                blogImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                blogImage.setBackgroundColor(Color.TRANSPARENT);
            } else {
                // Fallback to color background if no image is provided
                blogImage.setImageDrawable(null);
                try {
                    blogImage.setBackgroundColor(Color.parseColor(blog.getImageColor()));
                } catch (Exception e) {
                    blogImage.setBackgroundColor(Color.parseColor("#1565C0")); // Default color
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBlogClick(blog);
                }
            });
        }
    }
}