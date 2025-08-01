接口文档


url：http://192.168.3.78:48558


swagger: http://192.168.3.78:48558/docs


POST:/api/chat/stream

application/json

```json
{
    "messages": [
        {
            "role": "user",
            "content": "分析下周杰伦和刘德华共同有哪些共同好友？"
        }
    ],
    "thread_id": "fghp04MDG6-J3lYI9MWBw",
    "resources": [],
    "auto_accepted_plan": true,
    "enable_deep_thinking": false,
    "enable_background_investigation": false,
    "max_plan_iterations": 1,
    "max_step_num": 5,
    "max_search_results": 5,
    "report_style": "academic",
    "mcp_settings": {
        "servers": {
            "knowledge-graph-algorithrm-service": {
                "name": "knowledge-graph-algorithrm-service",
                "transport": "sse",
                "env": null,
                "url": "http://192.168.3.78:5821/sse",
                "enabled_tools": [
                    "most_recent_common_ancestor",
                    "relation_chain_between_stars",
                    "similarity_between_stars",
                    "mutual_friend_between_stars",
                    "dream_team_common_works",
                    "query_celebrity_relationships",
                   "recent_common_celebrity_event"
                ],
                "add_to_agents": [
                    "researcher",
                    "coder"
                ]
            }
        }
    }
}
```

