<template>
  <div class="comment-tree">
    <div v-for="c in items" :key="c.id" class="comment-item" :style="{ marginLeft: depth * 18 + 'px' }">
      <div class="row">
        <div class="left">
          <span class="user">{{ c.username || '用户' }}</span>
          <span class="time">{{ c.createTime }}</span>
        </div>
        <div class="right">
          <el-button v-if="canReply" link type="primary" @click="$emit('reply', c)">回复</el-button>
        </div>
      </div>
      <div class="content">{{ c.content }}</div>
      <CommentTree
        v-if="c.children && c.children.length"
        :items="c.children"
        :depth="depth + 1"
        :can-reply="canReply"
        @reply="$emit('reply', $event)"
      />
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'CommentTree' })

// 树形评论组件（递归组件）
// - items：评论数组，children 字段表示子评论
// - depth：递归深度，用于控制缩进
// - reply 事件：点击“回复”把被回复的评论对象抛给父组件
defineProps({
  items: { type: Array, default: () => [] },
  depth: { type: Number, default: 0 },
  canReply: { type: Boolean, default: false }
})

defineEmits(['reply'])
</script>

<style scoped>
.comment-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--sanguo-border);
}
.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}
.user {
  font-weight: 600;
  margin-right: 10px;
}
.time {
  font-size: 12px;
  color: var(--sanguo-text-secondary);
}
.content {
  margin-top: 6px;
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
