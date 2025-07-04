from fastapi import APIRouter,Request
from fastapi.responses import RedirectResponse,JSONResponse


router = APIRouter()


@router.get('/swagger-ui.html', include_in_schema=False)
async def redirect_to_docs(request: Request):
    return RedirectResponse(url=str(request.url).replace('swagger-ui.html', 'docs'))

@router.get('/health')
async def health():
    return JSONResponse(content={'status':'OK'},status_code=200)