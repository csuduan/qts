export default {
    path: "/system",
    redirect: "/error/403",
    meta: {
        icon: "setting",
        title: "系统",
        // showLink: false,
        rank: 1
    },
    children: [
        {
            "path": "/system/role/index",
            "name": "role",
            "meta": {
                "title": "角色管理"
            }
        },
        {
            "path": "/system/user/index",
            "name": "user",
            "meta": {
                "title": "用户管理"
            }
        },
        {
            "path": "/permission",
            "redirect": "/permission/page/index",
            "meta": {
                "title": "权限管理"
            },
            "children": [
                {
                    "path": "/permission/page/index",
                    "name": "permissionPage",
                    "meta": {
                        "title": "页面权限"
                    }
                },
                {
                    "path": "/permission/button/index",
                    "name": "permissionButton",
                    "meta": {
                        "title": "按钮权限"
                    }
                }
            ]
        },
        {
            path: "/error/403",
            name: "403",
            component: () => import("@/views/error/403.vue"),
            meta: {
                title: "403"
            }
        },
        {
            path: "/error/404",
            name: "404",
            component: () => import("@/views/error/404.vue"),
            meta: {
                title: "404"
            }
        },
        {
            path: "/error/500",
            name: "500",
            component: () => import("@/views/error/500.vue"),
            meta: {
                title: "500"
            }
        }
    ]
} as RouteConfigsTable;
