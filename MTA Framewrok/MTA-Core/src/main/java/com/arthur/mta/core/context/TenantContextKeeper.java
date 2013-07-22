package com.arthur.mta.core.context;



public class TenantContextKeeper {
	
	
	private static final ThreadLocal<TenantContext> contextRepository = new ThreadLocal<TenantContext>();

  
    public static void clearContext() {
    	contextRepository.remove();
    }

    public static TenantContext getContext() {
    	TenantContext ctx = contextRepository.get();

        if (ctx == null) {
            ctx = createEmptyContext();
            contextRepository.set(ctx);
        }
        

        return ctx;
    }

    public static void setContext(TenantContext context) {
    	contextRepository.set(context);
    }

    public static TenantContext createEmptyContext() {
        return new TenantContextImpl();
    }

}
