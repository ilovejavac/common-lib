package com.dev.lib.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Kahn {

	private final int len;

	private final Node[] nodes;

	@Getter
	@Setter
	static class Node {
		// 入度
		private Integer degree = 0;

		private List<Integer> to = new ArrayList<>();

	}

	public Kahn(int len) {

		this.len = len;
		nodes = new Node[len];
		for (int i = 0; i < len; i++) {
			nodes[i] = new Node();
		}
	}

	// prerequisites[i] = [u, v] 代表 u -> v
	public boolean hasCycle(List<int[]> prerequisites) {

		if (CollectionUtils.isEmpty(prerequisites)) {
			return false;
		}

		for (int[] edge : prerequisites) {
			int u  = edge[0];
			int v = edge[1];

			nodes[u].to.add(v);
			nodes[v].degree++;
		}

		Queue<Integer> queue = new LinkedList<>();
		for (int i = 0; i < len; i++) {
			if (nodes[i].degree == 0) {
				queue.offer(i);
			}
		}

		int count = 0;
		while (!queue.isEmpty()) {
			int u = queue.poll();
			count++;

			for (Integer v : nodes[u].to) {
				nodes[v].degree--;
				if (nodes[v].degree == 0) {
					queue.offer(v);
				}
			}
		}

		return count != len;
	}

}