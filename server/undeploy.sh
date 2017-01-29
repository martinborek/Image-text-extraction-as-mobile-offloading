kubectl delete -f cluster/backend.yaml
make -C cluster/sidecar/ delete-replica
make -C cluster/sidecar/ delete-replica
make -C cluster/sidecar/ delete-replica
gcloud container clusters delete backend -q
gsutil rm -r gs://eu.artifacts.mcc-2016-g13-p2.appspot.com/
echo 'Cluster and containers deleted.'