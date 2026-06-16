import { memo } from 'react';
import type { Post } from '../types/post';
import '../responsive.css';

interface PostItemProps {
  post: Post;
  onEdit: (post: Post) => void;
  onDelete: (id: number) => void;
  onTogglePublish: (id: number) => void;
  onLike: (id: number) => void;
}

const PostItem = memo(function PostItem({
  post,
  onEdit,
  onDelete,
  onTogglePublish,
  onLike,
}: PostItemProps) {
  return (
    <div className={`post-card ${post.isDeleted ? 'deleted' : ''}`}>
      <div className="post-header">
        <h3 className="post-title">{post.title}</h3>
        <span className="post-status">
          {post.isPublished ? '已发布' : '草稿'}
        </span>
      </div>

      <p className="post-content">{post.content}</p>

      <div className="post-meta">
        <span>{post.authorName}</span>
        <span>{post.viewCount} 次浏览</span>
        <span>{post.likeCount} 点赞</span>
        <span>{new Date(post.createdAt).toLocaleDateString()}</span>
      </div>

      <div className="post-actions">
        <button onClick={() => onLike(post.id)} className="btn-secondary">
          点赞
        </button>
        <button onClick={() => onTogglePublish(post.id)} className="btn-secondary">
          {post.isPublished ? '取消发布' : '发布'}
        </button>
        <button onClick={() => onEdit(post)} className="btn-primary">
          编辑
        </button>
        <button onClick={() => onDelete(post.id)} className="btn-danger">
          删除
        </button>
      </div>
    </div>
  );
});

export default PostItem;
