
#
# IDs used in this setup:
#
KSQLDB-APP    : lksqlc-28pgq
KAFKA-CLUSTER : lkc-jwgvw

#############################################################################################
# How to define acls for the KSQL-DB app?
##############################################################################################

ccloud ksql app configure-acls lksqlc-28pgq grid-link-flow-data --cluster lkc-jwgvw
ccloud ksql app configure-acls lksqlc-28pgq grid-plants --cluster lkc-jwgvw
ccloud ksql app configure-acls lksqlc-28pgq grid-regions --cluster lkc-jwgvw
ccloud ksql app configure-acls lksqlc-28pgq grid-stations --cluster lkc-jwgvw
ccloud ksql app configure-acls lksqlc-28pgq grid-static-links --cluster lkc-jwgvw

ccloud ksql app configure-acls lksqlc-28pgq grid-flow-enriched-sample-stream --cluster lkc-jwgvw

ccloud kafka acl create --allow --service-account 138337 --operation READ --operation WRITE --operation CREATE --topic '*' --cluster lkc-jwgvw

#############################################################################################
#  Which topics are used for simulation data?
#############################################################################################

# Clean up
ccloud kafka topic delete grid-regions --cluster lkc-jwgvw
ccloud kafka topic delete grid-stations --cluster lkc-jwgvw
ccloud kafka topic delete grid-plants --cluster lkc-jwgvw
ccloud kafka topic delete grid-link-flow-data --cluster lkc-jwgvw
ccloud kafka topic delete grid-static-links --cluster lkc-jwgvw

# Create
ccloud kafka topic create grid-regions --cluster lkc-jwgvw --partitions 1
ccloud kafka topic create grid-stations --cluster lkc-jwgvw --partitions 1
ccloud kafka topic create grid-plants --cluster lkc-jwgvw --partitions 1
ccloud kafka topic create grid-link-flow-data --cluster lkc-jwgvw --partitions 1
ccloud kafka topic create grid-static-links --cluster lkc-jwgvw --partitions 1



