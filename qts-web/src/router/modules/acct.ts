export default {
    path: "/acct",
    redirect: "/acct/view",
    meta: {
        icon: "chart-line",
        title: "账户",
        rank: 2
    },
    children: [
        {
            path: "/acct/view",
            name: "acct-view",
            component: () => import("@/views/acct/view/index.vue"),
            "meta": {
                "title": "账户视图",
            }

        }
        ,
        {
            path: "/acct/detail",
            name: "acct-detail",
            component: () => import("@/views/acct/detail/index.vue"),
            "meta": {
                "title": "账户详情",
                showLink: false
            }

        }

    ]
} as RouteConfigsTable;
