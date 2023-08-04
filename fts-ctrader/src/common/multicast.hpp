#pragma once
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <netdb.h>
#include <errno.h>
#include <iostream>

using namespace std;


int multicast(void)
{
    char group_ip[20];
    int group_port = 7838;
    int local_port = 11112;
    strcpy(group_ip,"230.1.1.1");



    char recmsg[256];


    int n;
    int socket_fd;
    struct sockaddr_in group_addr;  //group address
    struct sockaddr_in local_addr;//local address
    struct ip_mreq mreq;
    socklen_t addr_len = sizeof(group_addr);
    u_int yes;


    socket_fd=socket(AF_INET,SOCK_DGRAM,0);
    if(socket_fd < 0)
    {
        perror("socket multicast!");
        exit(1);
    }

    /* allow multiple sockets to use the same PORT number */
    if (setsockopt(socket_fd,SOL_SOCKET,SO_REUSEADDR,&yes,sizeof(yes)) < 0)
    {
        perror("Reusing ADDR failed");
        exit(1);
    }




    /*set up the destination address*/
    memset(&group_addr,0,sizeof(struct sockaddr_in));
    group_addr.sin_family = AF_INET;
    group_addr.sin_port = htons(group_port);
    group_addr.sin_addr.s_addr = inet_addr("230.1.1.1");




    /*set up the local address*/
    memset(&local_addr,0,sizeof(local_addr));
    local_addr.sin_family = AF_INET;
    local_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    local_addr.sin_port = htons(group_port);              //this port must be the group port
    /*bind local address*/
    if(bind(socket_fd,(struct sockaddr *)&local_addr,sizeof(local_addr)) == -1)
    {
        perror("Binding the multicast!");
        exit(1);
    }
    /*use the setsocketopt() to request joining the multicast group*/
    mreq.imr_multiaddr.s_addr=inet_addr(group_ip);
    mreq.imr_interface.s_addr=htonl(INADDR_ANY);
    if (setsockopt(socket_fd,IPPROTO_IP,IP_ADD_MEMBERSHIP,&mreq,sizeof(mreq)) < 0)
    {
        perror("setsockopt multicast!");
        exit(1);
    }
    /*loop to send or recieve*/
    while(1)
    {

        bzero(recmsg, sizeof(recmsg)-1);
        n = recvfrom(socket_fd, recmsg, sizeof(recmsg)-1, 0,
                     (struct sockaddr *) &group_addr, &addr_len);
        if (n < 0) {
            printf("recvfrom err in udptalk!\n");
            exit(4);
        } else {
            /* success recieve the information */
            recmsg[n] = 0;
            printf("peer:%s \n", recmsg);
        }

    }




}
