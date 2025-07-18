SELECT
    t2.NAME,
    t1.content
FROM
    mcp_tool_results t1
        LEFT JOIN tool_call_names t2 ON t1.tool_call_id = t2.call_id
WHERE
    t1.thread_id = 'analysis_1752819308392'
  AND
    t2.name = 'mutual_friend_between_stars'