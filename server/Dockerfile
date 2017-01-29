
FROM python:3-onbuild
RUN apt-get update && apt-get install -y --no-install-recommends apt-utils
RUN apt-get update && apt-get install -y tesseract-ocr
EXPOSE 443
CMD [ "python", "./server.py", "--logging=debug" ]
