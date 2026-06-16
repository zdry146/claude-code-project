import { useState, useEffect, useCallback, useRef } from 'react';
import { postApi, isSuccess } from './services/postApi';
import type { Post, CreatePostRequest, UpdatePostRequest } from './types/post';
import { PAGE_SIZE, DEFAULT_PAGE } from './constants';
import PostItem from './components/PostItem';
import PostForm from './components/PostForm';
import './responsive.css';

function App() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingPost, setEditingPost] = useState<Post | null>(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [currentPage, setCurrentPage] = useState(DEFAULT_PAGE);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [showAll, setShowAll] = useState(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  const fetchPosts = useCallback(async () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();

    setLoading(true);
    setError(null);
    try {
      const signal = abortControllerRef.current.signal;
      const result = showAll
        ? await postApi.listAll(currentPage, PAGE_SIZE, signal)
        : await postApi.listPublished(currentPage, PAGE_SIZE, signal);

      if (isSuccess(result)) {
        setPosts(result.data.content);
        setTotalPages(result.data.totalPages);
        setTotalElements(result.data.totalElements);
      } else {
        setError(result.message || '获取帖子失败');
      }
    } catch (err) {
      if (err instanceof Error && err.name !== 'AbortError') {
        setError('网络错误，请检查后端服务是否启动');
        console.error(err);
      }
    } finally {
      setLoading(false);
    }
  }, [currentPage, showAll]);

  useEffect(() => {
    fetchPosts();
    return () => {
      abortControllerRef.current?.abort();
    };
  }, [fetchPosts]);

  const handleSearch = async () => {
    if (!searchKeyword.trim()) {
      fetchPosts();
      return;
    }

    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();

    setLoading(true);
    setError(null);
    try {
      const signal = abortControllerRef.current.signal;
      const result = await postApi.search(searchKeyword, DEFAULT_PAGE, PAGE_SIZE, signal);
      if (isSuccess(result)) {
        setPosts(result.data.content);
        setTotalPages(result.data.totalPages);
        setTotalElements(result.data.totalElements);
        setCurrentPage(DEFAULT_PAGE);
      } else {
        setError(result.message || '搜索失败');
      }
    } catch (err) {
      if (err instanceof Error && err.name !== 'AbortError') {
        console.error('Search error:', err);
        setError('搜索失败，请重试');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleFormSubmit = async (data: CreatePostRequest | UpdatePostRequest) => {
    if (editingPost) {
      const updateData = data as UpdatePostRequest;
      try {
        const result = await postApi.update(editingPost.id, updateData);
        if (isSuccess(result)) {
          setEditingPost(null);
          setShowForm(false);
          fetchPosts();
        } else {
          alert(result.message || '更新失败');
        }
      } catch (err) {
        alert('更新失败，请重试');
        console.error(err);
      }
    } else {
      const createData = data as CreatePostRequest;
      try {
        const result = await postApi.create(createData);
        if (isSuccess(result)) {
          setShowForm(false);
          fetchPosts();
        } else {
          alert(result.message || '创建失败');
        }
      } catch (err) {
        alert('创建失败，请重试');
        console.error(err);
      }
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定要删除这篇帖子吗？')) return;

    try {
      const result = await postApi.delete(id);
      if (isSuccess(result)) {
        fetchPosts();
      } else {
        alert(result.message || '删除失败');
      }
    } catch (err) {
      alert('删除失败，请重试');
      console.error(err);
    }
  };

  const handleTogglePublish = async (id: number) => {
    try {
      const result = await postApi.togglePublish(id);
      if (isSuccess(result)) {
        fetchPosts();
      } else {
        alert(result.message || '操作失败');
      }
    } catch (err) {
      alert('操作失败，请重试');
      console.error(err);
    }
  };

  const handleLike = async (id: number) => {
    try {
      const result = await postApi.like(id);
      if (isSuccess(result)) {
        fetchPosts();
      } else {
        alert(result.message || '点赞失败');
      }
    } catch (err) {
      alert('点赞失败，请重试');
      console.error(err);
    }
  };

  const handleEdit = (post: Post) => {
    setEditingPost(post);
    setShowForm(true);
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  const handleCloseForm = () => {
    setShowForm(false);
    setEditingPost(null);
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <h1 className="app-title">帖子管理系统</h1>
        <p className="app-subtitle">调用 Spring Boot REST API 实现增删改查</p>
        <p className="app-total">共 {totalElements} 篇帖子</p>
      </header>

      <div className="controls">
        <div className="search-box">
          <input
            type="text"
            placeholder="搜索帖子..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            className="search-input"
            style={{ color: '#000000' }}
          />
          <button onClick={handleSearch} className="search-btn">搜索</button>
        </div>

        <div className="actions">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={showAll}
              onChange={(e) => {
                setShowAll(e.target.checked);
                setCurrentPage(DEFAULT_PAGE);
              }}
            />
            显示全部
          </label>
          <button onClick={() => { setShowForm(true); setEditingPost(null); }} className="create-btn">
            + 新建帖子
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner">
          <span>{error}</span>
          <button onClick={() => setError(null)} className="close-error">×</button>
        </div>
      )}

      {loading ? (
        <div className="loading">加载中...</div>
      ) : posts.length === 0 ? (
        <div className="empty">暂无帖子</div>
      ) : (
        <>
          <div className="post-list">
            {posts.map((post) => (
              <PostItem
                key={post.id}
                post={post}
                onEdit={handleEdit}
                onDelete={handleDelete}
                onTogglePublish={handleTogglePublish}
                onLike={handleLike}
              />
            ))}
          </div>

          {totalPages > 1 && (
            <div className="pagination">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="page-btn"
              >
                上一页
              </button>
              <span className="page-info">
                第 {currentPage + 1} / {totalPages} 页
              </span>
              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
                className="page-btn"
              >
                下一页
              </button>
            </div>
          )}
        </>
      )}

      {showForm && (
        <PostForm
          post={editingPost}
          onSubmit={handleFormSubmit}
          onCancel={handleCloseForm}
        />
      )}
    </div>
  );
}

export default App;
