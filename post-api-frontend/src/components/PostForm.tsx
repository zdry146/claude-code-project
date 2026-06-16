import { useEffect, type FC } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import type { Post, CreatePostRequest, UpdatePostRequest } from '../types/post';
import '../responsive.css';

const createPostSchema = z.object({
  title: z.string().min(1, '标题不能为空').max(200, '标题不能超过200字符'),
  content: z.string().min(1, '内容不能为空'),
  authorName: z.string().min(1, '作者名称不能为空').max(50, '作者名称不能超过50字符'),
  coverImage: z.string().url('请输入有效的URL').optional().or(z.literal('')),
});

const updatePostSchema = z.object({
  title: z.string().min(1, '标题不能为空').max(200, '标题不能超过200字符'),
  content: z.string().min(1, '内容不能为空'),
  authorName: z.string().min(1, '作者名称不能为空').max(50, '作者名称不能超过50字符').optional(),
  coverImage: z.string().url('请输入有效的URL').optional().or(z.literal('')),
  isPublished: z.boolean().optional(),
});

type CreateFormData = z.infer<typeof createPostSchema>;
type UpdateFormData = z.infer<typeof updatePostSchema>;

interface PostFormProps {
  post?: Post | null;
  onSubmit: (data: CreatePostRequest | UpdatePostRequest) => void;
  onCancel: () => void;
}

const PostForm: FC<PostFormProps> = ({ post, onSubmit, onCancel }) => {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateFormData | UpdateFormData>({
    defaultValues: {
      title: '',
      content: '',
      authorName: '',
      coverImage: '',
    },
  });

  useEffect(() => {
    if (post) {
      reset({
        title: post.title,
        content: post.content,
        authorName: post.authorName,
        coverImage: post.coverImage || '',
      });
    } else {
      reset({
        title: '',
        content: '',
        authorName: '',
        coverImage: '',
      });
    }
  }, [post, reset]);

  const onValid = (data: CreateFormData | UpdateFormData) => {
    if (post) {
      const updateData: UpdatePostRequest = {
        title: data.title,
        content: data.content,
        coverImage: data.coverImage || undefined,
      };
      onSubmit(updateData);
    } else {
      const createData: CreatePostRequest = {
        title: data.title,
        content: data.content,
        authorName: data.authorName,
        coverImage: data.coverImage || undefined,
      };
      onSubmit(createData);
    }
  };

  return (
    <div className="form-overlay">
      <div className="form-modal">
        <h2 className="form-title">{post ? '编辑帖子' : '新建帖子'}</h2>

        <form onSubmit={handleSubmit(onValid)}>
          {!post && (
            <div className="form-field">
              <label className="form-label">作者名称 *</label>
              <input
                type="text"
                {...register('authorName', { required: '作者名称不能为空' })}
                className={`form-input ${errors.authorName ? 'error' : ''}`}
              />
              {errors.authorName && (
                <span className="error-text">{errors.authorName.message}</span>
              )}
            </div>
          )}

          <div className="form-field">
            <label className="form-label">标题 *</label>
            <input
              type="text"
              {...register('title', { required: '标题不能为空' })}
              className={`form-input ${errors.title ? 'error' : ''}`}
            />
            {errors.title && (
              <span className="error-text">{errors.title.message}</span>
            )}
          </div>

          <div className="form-field">
            <label className="form-label">内容 *</label>
            <textarea
              {...register('content', { required: '内容不能为空' })}
              className={`form-textarea ${errors.content ? 'error' : ''}`}
              rows={6}
            />
            {errors.content && (
              <span className="error-text">{errors.content.message}</span>
            )}
          </div>

          <div className="form-field">
            <label className="form-label">封面图片 URL</label>
            <input
              type="text"
              {...register('coverImage')}
              className={`form-input ${errors.coverImage ? 'error' : ''}`}
              placeholder="可选"
            />
            {errors.coverImage && (
              <span className="error-text">{errors.coverImage.message}</span>
            )}
          </div>

          <div className="form-actions">
            <button type="submit" disabled={isSubmitting} className="btn-primary">
              {isSubmitting ? '提交中...' : post ? '保存' : '创建'}
            </button>
            <button type="button" onClick={onCancel} className="btn-secondary">
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default PostForm;
